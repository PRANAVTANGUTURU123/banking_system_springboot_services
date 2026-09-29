package com.billpay.worker.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository repo;
    private final ObjectMapper objectMapper;

    /** Must join the caller's transaction, so the event commits (or rolls back) with the state change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String key, Object payload) {
        try {
            repo.save(OutboxEvent.builder()
                    .topic(topic)
                    .key(key)
                    .payload(objectMapper.writeValueAsString(payload))
                    .state("PENDING")
                    .createdAt(OffsetDateTime.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event for topic " + topic, e);
        }
    }
}
