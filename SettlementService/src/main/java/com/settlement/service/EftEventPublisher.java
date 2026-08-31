package com.settlement.service;

import com.events.eft.EftBatchSubmittedEvent;
import com.events.eft.EftStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EftEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize {} event to JSON", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public void publishBatchSubmitted(EftBatchSubmittedEvent event) {
        log.info("Emitting eft.batch.submitted for batchId={}", event.batchId());
        kafkaTemplate.send("eft.batch.submitted", event.batchId().toString(), toJson(event));
    }

    public void publishEftStatus(EftStatusEvent event) {
        log.info("Emitting eft.status for paymentId={} batchId={} status={}",
                event.paymentId(), event.batchId(), event.status());
        kafkaTemplate.send("eft.status", event.paymentId().toString(), toJson(event));
    }
}
