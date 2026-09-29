package com.settlement.service;

import com.events.BatchDeadLettered;
import com.events.eft.EftAckMessage;
import com.events.eft.EftBatchReadyEvent;
import com.events.eft.EftBatchSubmittedEvent;
import com.events.eft.EftStatusEvent;
import com.settlement.domain.EftBatchSettlement;
import com.settlement.domain.SettlementStatus;
import com.settlement.repo.EftBatchSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * EFT settlement: builds a mock CPA-005 file for a ready batch, "uploads" it
 * to the EFT network, and translates network acks into per-payment status events.
 * Retry/dead-letter behaviour mirrors {@link SettlementProcessor}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EftSettlementProcessor {

    private final EftBatchSettlementRepository settlementRepo;
    private final EftNetworkClient networkClient;
    private final EftEventPublisher eventPublisher;
    private final RetryPolicy retryPolicy;

    @Transactional
    public void processNewBatch(EftBatchReadyEvent event) {
        UUID batchId = event.batchId();
        EftBatchSettlement settlement = settlementRepo.findByBatchId(batchId)
                .orElseGet(() -> {
                    EftBatchSettlement s = new EftBatchSettlement();
                    s.setBatchId(batchId);
                    s.setCreatedAt(OffsetDateTime.now());
                    s.setRetryCount(0);
                    s.setStatus(SettlementStatus.READY);
                    return s;
                });

        // Redelivered ready event: retries of failed uploads belong to the retry scheduler.
        if (settlement.getStatus() != SettlementStatus.READY
                && settlement.getStatus() != SettlementStatus.FILE_BUILT) {
            log.info("EFT batch {} already {}, ignoring duplicate eft.batch.ready", batchId, settlement.getStatus());
            return;
        }

        log.info("Processing NEW EFT batchId={} ({} lines)", batchId, event.lineCount());
        attemptUpload(settlement);
    }

    @Scheduled(fixedDelayString = "${settlement.retry.check-interval-ms:2000}")
    @Transactional
    public void retryDueBatches() {
        for (EftBatchSettlement s : settlementRepo.findByStatusAndNextRetryAtLessThanEqual(
                SettlementStatus.FAILED, OffsetDateTime.now())) {
            log.info("Retrying EFT batchId={} (attempt {})", s.getBatchId(), s.getRetryCount() + 1);
            attemptUpload(s);
        }
    }

    private void attemptUpload(EftBatchSettlement settlement) {
        UUID batchId = settlement.getBatchId();

        if (settlement.getCpa005FileName() == null) {
            String fileName = "cpa005-" + batchId + ".txt";
            log.info("Building CPA-005 file={} for batchId={}", fileName, batchId);
            settlement.setCpa005FileName(fileName);
            settlement.setStatus(SettlementStatus.FILE_BUILT);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);
        }

        try {
            String networkRef = networkClient.upload(settlement.getCpa005FileName());
            settlement.setNetworkReference(networkRef);
            settlement.setStatus(SettlementStatus.SUBMITTED);
            settlement.setNextRetryAt(null);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);

            eventPublisher.publishBatchSubmitted(
                    new EftBatchSubmittedEvent(UUID.randomUUID().toString(), batchId, networkRef));

        } catch (Exception ex) {
            int failures = settlement.getRetryCount() + 1;
            settlement.setRetryCount(failures);
            settlement.setLastError(ex.getMessage());
            settlement.setUpdatedAt(OffsetDateTime.now());

            if (retryPolicy.exhausted(failures)) {
                log.warn("EFT upload failed for batchId={} ({} attempts), dead-lettering: {}", batchId, failures, ex.getMessage());
                settlement.setStatus(SettlementStatus.DEAD_LETTERED);
                settlement.setNextRetryAt(null);
                eventPublisher.publishDlq(new BatchDeadLettered(
                        UUID.randomUUID().toString(), batchId, failures, ex.getMessage(), OffsetDateTime.now()));
            } else {
                settlement.setStatus(SettlementStatus.FAILED);
                settlement.setNextRetryAt(retryPolicy.nextAttemptAt(failures));
                log.warn("EFT upload failed for batchId={} (attempt {}), retry at {}: {}",
                        batchId, failures, settlement.getNextRetryAt(), ex.getMessage());
            }
            settlementRepo.save(settlement);
        }
    }

    @Transactional
    public void handleAck(EftAckMessage msg) {
        log.info("Handling EFT ack for paymentId={} batchId={}", msg.paymentId(), msg.batchId());

        String status = msg.success() ? "POSTED" : "FAILED";

        eventPublisher.publishEftStatus(new EftStatusEvent(
                // Deterministic per payment, so a redelivered ack is deduped downstream
                UUID.nameUUIDFromBytes(("eft.status:" + msg.paymentId()).getBytes(StandardCharsets.UTF_8)),
                msg.paymentId(),
                msg.batchId(),
                status,
                msg.reason(),
                OffsetDateTime.now()
        ));
    }
}
