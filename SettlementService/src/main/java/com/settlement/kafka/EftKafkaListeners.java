package com.settlement.kafka;

import com.events.eft.EftAckFileMessage;
import com.events.eft.EftAckMessage;
import com.events.eft.EftBatchReadyEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlement.service.EftSettlementProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EftKafkaListeners {

    private final EftSettlementProcessor eftProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "eft.batch.ready", groupId = "settlement-service")
    public void onEftBatchReady(String payload) {
        try {
            EftBatchReadyEvent event = objectMapper.readValue(payload, EftBatchReadyEvent.class);
            log.info("Received eft.batch.ready for batchId={}", event.batchId());
            eftProcessor.processNewBatch(event);
        } catch (Exception e) {
            log.error("Failed to parse EftBatchReadyEvent from payload: {}", payload, e);
            throw new RuntimeException("Failed to handle eft.batch.ready", e);
        }
    }

    @KafkaListener(topics = "eftnetwork.ack", groupId = "settlement-service")
    public void onEftAck(String payload) {
        try {
            EftAckFileMessage file = objectMapper.readValue(payload, EftAckFileMessage.class);
            log.info("Received EFT ack file for batchId={} with {} items",
                    file.batchId(), file.items().size());

            for (EftAckMessage msg : file.items()) {
                eftProcessor.handleAck(msg);
            }
        } catch (Exception e) {
            log.error("Failed to parse EftAckFileMessage from payload: {}", payload, e);
            throw new RuntimeException("Failed to handle eftnetwork.ack", e);
        }
    }
}
