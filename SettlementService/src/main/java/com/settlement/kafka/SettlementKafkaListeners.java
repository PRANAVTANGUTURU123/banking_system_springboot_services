package com.settlement.kafka;

import com.billpay.dto.Pain002FileMessage;
import com.events.Topics;
import com.events.billpay.BillBatchReadyEvent;
import com.events.billpay.Pain002Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlement.service.SettlementProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/** Exceptions propagate to the container's error handler, which retries the record. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementKafkaListeners {

    private final SettlementProcessor settlementProcessor;
    private final ObjectMapper objectMapper; 

    @KafkaListener(topics = Topics.BILL_BATCH_READY, groupId = "settlement-service")
    public void onBatchReady(String payload) throws Exception {
        BillBatchReadyEvent event = objectMapper.readValue(payload, BillBatchReadyEvent.class);
        log.info("Received bill.batch.ready for batchId={}", event.batchId());
        settlementProcessor.processNewBatch(event);
    }

    @KafkaListener(topics = Topics.CENTRAL1_PAIN002, groupId = "settlement-service")
    public void onPain002(String payload) throws Exception {
        Pain002FileMessage file = objectMapper.readValue(payload, Pain002FileMessage.class);

        log.info("Received pain.002 file for batchId={} with {} items",
                file.batchId(), file.items().size());

        // If this fails part-way the whole file is redelivered; status event ids are
        // deterministic per payment, so the already-emitted ones are deduped downstream.
        for (Pain002Message msg : file.items()) {
            settlementProcessor.handlePain002(msg);
        }
    }
}
