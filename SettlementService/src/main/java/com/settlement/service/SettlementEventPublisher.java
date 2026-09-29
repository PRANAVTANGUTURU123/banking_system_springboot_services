package com.settlement.service;

import com.events.BatchDeadLettered;
import com.events.Topics;
import com.events.billpay.BillBatchSubmittedEvent;
import com.events.billpay.BillpayStatusEvent;
import com.settlement.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bill-pay rail events. Everything goes through the outbox, so an event is
 * published if and only if the settlement state change that caused it commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventPublisher {

    private final OutboxWriter outbox;

    public void publishBatchSubmitted(BillBatchSubmittedEvent event) {
        log.info("Emitting bill.batch.submitted for batchId={}", event.batchId());
        outbox.enqueue(Topics.BILL_BATCH_SUBMITTED, event.batchId().toString(), event);
    }

    public void publishDlq(BatchDeadLettered event) {
        log.warn("Emitting bill.batch.dlq for batchId={} reason={}", event.batchId(), event.reason());
        outbox.enqueue(Topics.BILL_BATCH_DLQ, event.batchId().toString(), event);
    }

    public void publishBillpayStatus(BillpayStatusEvent event) {
        log.info("Emitting billpay.status for paymentId={} batchId={} status={}",
                event.paymentId(), event.batchId(), event.status());
        outbox.enqueue(Topics.BILLPAY_STATUS, event.paymentId().toString(), event);
    }
}
