package com.settlement.service;

import com.events.BatchDeadLettered;
import com.events.Topics;
import com.events.eft.EftBatchSubmittedEvent;
import com.events.eft.EftStatusEvent;
import com.settlement.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** EFT rail events, published through the outbox (see {@link SettlementEventPublisher}). */
@Component
@RequiredArgsConstructor
@Slf4j
public class EftEventPublisher {

    private final OutboxWriter outbox;

    public void publishBatchSubmitted(EftBatchSubmittedEvent event) {
        log.info("Emitting eft.batch.submitted for batchId={}", event.batchId());
        outbox.enqueue(Topics.EFT_BATCH_SUBMITTED, event.batchId().toString(), event);
    }

    public void publishDlq(BatchDeadLettered event) {
        log.warn("Emitting eft.batch.dlq for batchId={} reason={}", event.batchId(), event.reason());
        outbox.enqueue(Topics.EFT_BATCH_DLQ, event.batchId().toString(), event);
    }

    public void publishEftStatus(EftStatusEvent event) {
        log.info("Emitting eft.status for paymentId={} batchId={} status={}",
                event.paymentId(), event.batchId(), event.status());
        outbox.enqueue(Topics.EFT_STATUS, event.paymentId().toString(), event);
    }
}
