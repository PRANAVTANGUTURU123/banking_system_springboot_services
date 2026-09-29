package com.eftworker.service;

import com.eftworker.domain.EftBatch;
import com.eftworker.domain.EftBatchLine;
import com.eftworker.outbox.OutboxWriter;
import com.eftworker.repo.EftBatchLineRepository;
import com.eftworker.repo.EftBatchRepository;
import com.events.Topics;
import com.events.eft.EftBatchReadyEvent;
import com.events.eft.EftEnqueued;
import com.events.eft.EftRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Files EFT payments into batches (mirrors BillPayWorkerService). A batch is
 * closed (eft.batch.ready) at {@code batching.max-size} lines or after
 * {@code batching.max-age}, whichever comes first.
 */
@Service
public class EftWorkerService {

    private static final Logger log = LoggerFactory.getLogger(EftWorkerService.class);

    private static final String OPEN = "OPEN";
    private static final String READY_EMITTED = "READY_EMITTED";

    private final EftBatchRepository batches;
    private final EftBatchLineRepository lines;
    private final OutboxWriter outbox;

    @Value("${batching.max-size:50}")
    private int maxBatchSize;

    @Value("${batching.max-age:15s}")
    private Duration maxBatchAge;

    public EftWorkerService(EftBatchRepository batches,
                            EftBatchLineRepository lines,
                            OutboxWriter outbox) {
        this.batches = batches;
        this.lines = lines;
        this.outbox = outbox;
    }

    @Transactional
    public void handleRequested(EftRequested evt) {
        // Idempotent consumer: a redelivered request must not be batched twice.
        if (lines.existsByPaymentId(evt.paymentId())) {
            log.info("EFT payment {} already batched, skipping redelivery", evt.paymentId());
            return;
        }

        // 1) find (and lock) the OPEN batch, or open a new one
        EftBatch batch = batches.findFirstByStatusOrderByCreatedAtAsc(OPEN)
                .orElseGet(() -> {
                    EftBatch b = new EftBatch(UUID.randomUUID(), OPEN, OffsetDateTime.now());
                    batches.save(b);
                    log.info("Opened new EFT batch {}", b.getBatchId());
                    return b;
                });

        int nextLineNo = (int) lines.countByBatchId(batch.getBatchId()) + 1;
        lines.save(new EftBatchLine(batch.getBatchId(), evt.paymentId(), nextLineNo, OffsetDateTime.now()));
        log.info("Enqueued EFT payment {} into batch {} lineNo={}",
                evt.paymentId(), batch.getBatchId(), nextLineNo);

        // 2) eft.enqueued — via the outbox, committed with the batch line
        outbox.enqueue(Topics.EFT_ENQUEUED, evt.paymentId().toString(), new EftEnqueued(
                UUID.randomUUID().toString(),
                evt.paymentId(),
                batch.getBatchId(),
                OffsetDateTime.now().toString(),
                "1",
                "eft"
        ));

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
        for (EftBatch batch : batches.findByStatusAndCreatedAtBefore(OPEN, cutoff)) {
            close(batch, (int) lines.countByBatchId(batch.getBatchId()), "age");
        }
    }

    private void close(EftBatch batch, int lineCount, String trigger) {
        batch.setStatus(READY_EMITTED);
        batches.save(batch);

        outbox.enqueue(Topics.EFT_BATCH_READY, batch.getBatchId().toString(), new EftBatchReadyEvent(
                UUID.randomUUID().toString(),
                batch.getBatchId(),
                lineCount,
                OffsetDateTime.now().toString(),
                "1",
                "eft"
        ));
        log.info("Closed EFT batch {} ({} cutoff, {} lines), emitted eft.batch.ready",
                batch.getBatchId(), trigger, lineCount);
    }
}
