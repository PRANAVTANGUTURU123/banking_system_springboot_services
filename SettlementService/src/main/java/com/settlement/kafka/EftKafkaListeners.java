package com.settlement.kafka;

import com.events.Topics;
import com.events.eft.EftAckFileMessage;
import com.events.eft.EftAckMessage;
import com.events.eft.EftBatchReadyEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlement.service.EftSettlementProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/** Exceptions propagate to the container's error handler, which retries the record. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EftKafkaListeners {

    private final EftSettlementProcessor eftProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.EFT_BATCH_READY, groupId = "settlement-service")
    public void onEftBatchReady(String payload) throws Exception {
        EftBatchReadyEvent event = objectMapper.readValue(payload, EftBatchReadyEvent.class);
        log.info("Received eft.batch.ready for batchId={}", event.batchId());
        eftProcessor.processNewBatch(event);
    }

    @KafkaListener(topics = Topics.EFTNETWORK_ACK, groupId = "settlement-service")
    public void onEftAck(String payload) throws Exception {
        EftAckFileMessage file = objectMapper.readValue(payload, EftAckFileMessage.class);
        log.info("Received EFT ack file for batchId={} with {} items",
                file.batchId(), file.items().size());

        for (EftAckMessage msg : file.items()) {
            eftProcessor.handleAck(msg);
        }
    }
}
