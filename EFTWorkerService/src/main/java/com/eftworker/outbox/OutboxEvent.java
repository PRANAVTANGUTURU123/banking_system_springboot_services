package com.eftworker.outbox;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Transactional outbox row: written in the same DB transaction as the state
 * change that produced the event, relayed to Kafka by {@link OutboxPublisher}.
 */
@Entity
@Table(name = "outbox_event", indexes = @Index(name = "idx_outbox_state", columnList = "state, id"))
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 120)
    private String key;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, length = 20)
    private String state; // PENDING -> PUBLISHED

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime publishedAt;

    public OutboxEvent() {}

    public OutboxEvent(String topic, String key, String payload) {
        this.topic = topic;
        this.key = key;
        this.payload = payload;
        this.state = "PENDING";
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getTopic() { return topic; }
    public String getKey() { return key; }
    public String getPayload() { return payload; }
    public String getState() { return state; }

    public void markPublished() {
        this.state = "PUBLISHED";
        this.publishedAt = OffsetDateTime.now();
    }
}
