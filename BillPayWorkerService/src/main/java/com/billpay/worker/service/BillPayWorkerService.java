package com.billpay.worker.service;

import com.billpay.worker.domain.Batch;
import com.billpay.worker.domain.BatchLine;
import com.billpay.worker.outbox.OutboxWriter;
import com.events.Topics;
import com.events.billpay.*;
import com.billpay.worker.repo.BatchLineRepository;
import com.billpay.worker.repo.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Files bill payments into batches. A batch is closed (bill.batch.ready) when it
 * reaches {@code batching.max-size} lines or has been open for
 * {@code batching.max-age}, whichever comes first — like a real clearing cutoff.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillPayWorkerService {

    private static final String OPEN = "OPEN";
    private static final String READY_EMITTED = "READY_EMITTED";

    private final BatchRepository batches;
    private final BatchLineRepository lines;
    private final OutboxWriter outbox;

    @Value("${batching.max-size:50}")
    private int maxBatchSize;

    @Value("${batching.max-age:15s}")
    private Duration maxBatchAge;

    @Transactional
    public void handleRequested(BillPayRequested evt) {
        // Idempotent consumer: a redelivered request must not be batched twice.
        // (The unique constraint on batch_lines.payment_id backs this up under races.)
        if (lines.existsByPaymentId(evt.getPaymentId())) {
            log.info("Payment {} already batched, skipping redelivery", evt.getPaymentId());
            return;
        }

        // 1) find (and lock) the OPEN batch, or open a new one
        Batch batch = batches.findFirstByStatusOrderByCreatedAtAsc(OPEN)
                .orElseGet(() -> {
                    Batch b = Batch.builder()
                            .batchId(UUID.randomUUID())
                            .status(OPEN)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    batches.save(b);
                    log.info("Opened new batch {}", b.getBatchId());
                    return b;
                });

        int nextLineNo = (int) lines.countByBatchId(batch.getBatchId()) + 1;
        lines.save(BatchLine.builder()
                .batchId(batch.getBatchId())
                .paymentId(evt.getPaymentId())
                .lineNo(nextLineNo)
                .createdAt(OffsetDateTime.now())
                .build());

        log.info("Enqueued payment {} into batch {} lineNo={}", evt.getPaymentId(), batch.getBatchId(), nextLineNo);

        // 2) billpay.enqueued — via the outbox, committed with the batch line
        outbox.enqueue(Topics.BILLPAY_ENQUEUED, evt.getPaymentId().toString(), BillPayEnqueued.builder()
                .eventId(UUID.randomUUID().toString())
                .paymentId(evt.getPaymentId())
                .batchId(batch.getBatchId())
                .correlationId(evt.getCorrelationId())
                .occurredAt(OffsetDateTime.now().toString())
                .schemaVersion("1")
                .channel("billpay")
                .build());

        // 3) size cutoff
        if (nextLineNo >= maxBatchSize) {
            close(batch, nextLineNo, "size");
        }
    }

    /** Age cutoff: close any OPEN batch older than max-age. */
    @Scheduled(fixedDelayString = "${batching.check-interval-ms:5000}")
    @Transactional
    public void closeExpiredBatches() {
        var cutoff = OffsetDateTime.now().minus(maxBatchAge);
        for (Batch batch : batches.findByStatusAndCreatedAtBefore(OPEN, cutoff)) {
            close(batch, (int) lines.countByBatchId(batch.getBatchId()), "age");
        }
    }

    private void close(Batch batch, int lineCount, String trigger) {
        batch.setStatus(READY_EMITTED);
        batches.save(batch);

        outbox.enqueue(Topics.BILL_BATCH_READY, batch.getBatchId().toString(), BillBatchReady.builder()
                .eventId(UUID.randomUUID().toString())
                .batchId(batch.getBatchId())
                .lineCount(lineCount)
                .occurredAt(OffsetDateTime.now().toString())
                .schemaVersion("1")
                .channel("billpay")
                .build());
        log.info("Closed batch {} ({} cutoff, {} lines), emitted bill.batch.ready", batch.getBatchId(), trigger, lineCount);
    }
}
