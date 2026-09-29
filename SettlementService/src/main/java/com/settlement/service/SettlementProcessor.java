package com.settlement.service;

import com.events.BatchDeadLettered;
import com.events.billpay.BillBatchReadyEvent;
import com.events.billpay.Pain002Message;
import com.settlement.domain.BillBatchSettlement;
import com.settlement.domain.SettlementStatus;
import com.settlement.repo.BillBatchSettlementRepository;
import com.events.billpay.BillpayStatusEvent;
import com.events.billpay.BillBatchSubmittedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bill-pay settlement: build the pain.001 file for a ready batch, upload it to
 * Central1, and translate pain.002 results into per-payment status events.
 *
 * <p>A failed upload is retried by {@link #retryDueBatches()} with exponential
 * backoff (state lives in the DB, so retries survive restarts). When retries are
 * exhausted the batch is DEAD_LETTERED and bill.batch.dlq tells the orchestrator
 * to fail its payments and release their holds.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementProcessor {

    private final BillBatchSettlementRepository settlementRepo;
    private final Pain001Builder pain001Builder;
    private final Central1Client central1Client;
    private final SettlementEventPublisher eventPublisher;
    private final RetryPolicy retryPolicy;

    @Transactional
    public void processNewBatch(BillBatchReadyEvent event) {
        UUID batchId = event.batchId();
        BillBatchSettlement settlement = settlementRepo.findByBatchId(batchId)
            .orElseGet(() -> {
                BillBatchSettlement s = new BillBatchSettlement();
                s.setBatchId(batchId);
                s.setCreatedAt(OffsetDateTime.now());
                s.setRetryCount(0);
                s.setStatus(SettlementStatus.READY);
                return s;
            });

        // Redelivered ready event: only a batch that hasn't been attempted yet is
        // processed here; retries of failed uploads belong to the retry scheduler.
        if (settlement.getStatus() != SettlementStatus.READY
                && settlement.getStatus() != SettlementStatus.FILE_BUILT) {
            log.info("Batch {} already {}, ignoring duplicate bill.batch.ready", batchId, settlement.getStatus());
            return;
        }

        log.info("Processing NEW batchId={} ({} lines)", batchId, event.lineCount());
        attemptUpload(settlement);
    }

    @Scheduled(fixedDelayString = "${settlement.retry.check-interval-ms:2000}")
    @Transactional
    public void retryDueBatches() {
        for (BillBatchSettlement s : settlementRepo.findByStatusAndNextRetryAtLessThanEqual(
                SettlementStatus.FAILED, OffsetDateTime.now())) {
            log.info("Retrying batchId={} (attempt {})", s.getBatchId(), s.getRetryCount() + 1);
            attemptUpload(s);
        }
    }

    private void attemptUpload(BillBatchSettlement settlement) {
        UUID batchId = settlement.getBatchId();

        // Build file only if not already built
        if (settlement.getPain001FileName() == null) {
            settlement.setPain001FileName(pain001Builder.buildFileForBatch(batchId));
            settlement.setStatus(SettlementStatus.FILE_BUILT);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);
        }

        try {
            String centralRef = central1Client.upload(settlement.getPain001FileName());
            settlement.setCentralReference(centralRef);
            settlement.setStatus(SettlementStatus.SUBMITTED);
            settlement.setNextRetryAt(null);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);

            // Outbox: commits together with SUBMITTED
            eventPublisher.publishBatchSubmitted(new BillBatchSubmittedEvent(batchId, centralRef));

        } catch (Exception ex) {
            int failures = settlement.getRetryCount() + 1;
            settlement.setRetryCount(failures);
            settlement.setLastError(ex.getMessage());
            settlement.setUpdatedAt(OffsetDateTime.now());

            if (retryPolicy.exhausted(failures)) {
                log.warn("Upload failed for batchId={} ({} attempts), dead-lettering: {}", batchId, failures, ex.getMessage());
                settlement.setStatus(SettlementStatus.DEAD_LETTERED);
                settlement.setNextRetryAt(null);
                eventPublisher.publishDlq(new BatchDeadLettered(
                        UUID.randomUUID().toString(), batchId, failures, ex.getMessage(), OffsetDateTime.now()));
            } else {
                settlement.setStatus(SettlementStatus.FAILED);
                settlement.setNextRetryAt(retryPolicy.nextAttemptAt(failures));
                log.warn("Upload failed for batchId={} (attempt {}), retry at {}: {}",
                        batchId, failures, settlement.getNextRetryAt(), ex.getMessage());
            }
            settlementRepo.save(settlement);
        }
    }

    @Transactional
    public void handlePain002(Pain002Message msg) {
        log.info("Handling pain.002 for paymentId={} batchId={}", msg.paymentId(), msg.batchId());

        String status = msg.success() ? "POSTED" : "FAILED";

        eventPublisher.publishBillpayStatus(new BillpayStatusEvent(
                statusEventId(msg.paymentId()),
                msg.paymentId(),
                msg.batchId(),
                status,
                msg.reason(),
                OffsetDateTime.now()
        ));
    }

    /**
     * One final status per payment, so its event id is derived from the payment id:
     * a redelivered pain.002 yields the same id and the orchestrator's dedupe drops it.
     */
    private static UUID statusEventId(UUID paymentId) {
        return UUID.nameUUIDFromBytes(("billpay.status:" + paymentId).getBytes(StandardCharsets.UTF_8));
    }
}
