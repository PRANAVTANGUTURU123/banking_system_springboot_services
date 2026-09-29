package com.eftworker.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxEventRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${outbox.publish.fixed-delay-ms:1000}")
    @Transactional
    public void publish() {
        for (OutboxEvent row : repo.lockPendingBatch()) {
            try {
                kafka.send(row.getTopic(), row.getKey(), row.getPayload()).get();
            } catch (Exception e) {
                // Stay PENDING and retry next run; stop so later events don't overtake this one.
                log.warn("Outbox publish failed id={} topic={}, will retry: {}", row.getId(), row.getTopic(), e.getMessage());
                break;
            }
            row.markPublished();
        }
    }
}
