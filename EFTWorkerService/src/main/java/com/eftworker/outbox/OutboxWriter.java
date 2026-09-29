package com.eftworker.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWriter {

    private final OutboxEventRepository repo;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxEventRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    /** Must join the caller's transaction, so the event commits (or rolls back) with the state change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String key, Object payload) {
        try {
            repo.save(new OutboxEvent(topic, key, objectMapper.writeValueAsString(payload)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event for topic " + topic, e);
        }
    }
}
