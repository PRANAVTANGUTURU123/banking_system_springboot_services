package com.settlement.service;

import com.events.eft.EftAckMessage;
import com.events.eft.EftBatchReadyEvent;
import com.events.eft.EftBatchSubmittedEvent;
import com.events.eft.EftStatusEvent;
import com.settlement.domain.EftBatchSettlement;
import com.settlement.domain.SettlementStatus;
import com.settlement.repo.EftBatchSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * EFT settlement: builds a mock CPA-005 file for a ready batch, "uploads" it
 * to the EFT network, and translates network acks into per-payment status events.
 *
 * <p>Happy path only for now — no retry/DLQ (unlike the BillPay processor).
 * A failed upload is marked FAILED and logged; re-drive it by re-sending the
 * eft.batch.ready event.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EftSettlementProcessor {

    private final EftBatchSettlementRepository settlementRepo;
    private final EftEventPublisher eventPublisher;

    @Transactional
    public void processNewBatch(EftBatchReadyEvent event) {
        UUID batchId = event.batchId();
        log.info("Processing NEW EFT batchId={}", batchId);

        EftBatchSettlement settlement = settlementRepo.findByBatchId(batchId)
                .orElseGet(() -> {
                    EftBatchSettlement s = new EftBatchSettlement();
                    s.setBatchId(batchId);
                    s.setCreatedAt(OffsetDateTime.now());
                    s.setStatus(SettlementStatus.READY);
                    return s;
                });

        // Idempotency guard: already uploaded/submitted? don't redo.
        if ((settlement.getStatus() == SettlementStatus.UPLOADED
                || settlement.getStatus() == SettlementStatus.SUBMITTED)
                && settlement.getNetworkReference() != null) {
            log.info("EFT batch {} already uploaded/submitted (networkRef={}), skipping.",
                    batchId, settlement.getNetworkReference());
            return;
        }

        if (settlement.getCpa005FileName() == null) {
            String fileName = "cpa005-" + batchId + ".txt";
            log.info("Building CPA-005 file={} for batchId={}", fileName, batchId);
            settlement.setCpa005FileName(fileName);
            settlement.setStatus(SettlementStatus.FILE_BUILT);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);
        }

        try {
            log.info("Uploading CPA-005 file={} to EFT network...", settlement.getCpa005FileName());
            String networkRef = "EFTNET-" + System.currentTimeMillis();

            settlement.setNetworkReference(networkRef);
            settlement.setStatus(SettlementStatus.UPLOADED);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);

            eventPublisher.publishBatchSubmitted(
                    new EftBatchSubmittedEvent(UUID.randomUUID().toString(), batchId, networkRef));

            settlement.setStatus(SettlementStatus.SUBMITTED);
            settlementRepo.save(settlement);

        } catch (Exception ex) {
            // Happy-path implementation: no retry/DLQ yet — mark FAILED and log.
            log.error("EFT upload failed for batchId={} : {}", batchId, ex.getMessage(), ex);
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setLastError(ex.getMessage());
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);
        }
    }

    @Transactional
    public void handleAck(EftAckMessage msg) {
        log.info("Handling EFT ack for paymentId={} batchId={}", msg.paymentId(), msg.batchId());

        String status = msg.success() ? "POSTED" : "FAILED";

        eventPublisher.publishEftStatus(new EftStatusEvent(
                UUID.randomUUID(),
                msg.paymentId(),
                msg.batchId(),
                status,
                msg.reason(),
                OffsetDateTime.now()
        ));
    }
}
