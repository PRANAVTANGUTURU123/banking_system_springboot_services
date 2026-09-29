package com.billpay.worker.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Transactional outbox row: written in the same DB transaction as the state
 * change that produced the event, relayed to Kafka by {@link OutboxPublisher}.
 */
@Entity
@Table(name = "outbox_event", indexes = @Index(name = "idx_outbox_state", columnList = "state, id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
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
}
