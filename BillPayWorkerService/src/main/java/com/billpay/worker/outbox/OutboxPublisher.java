package com.billpay.worker.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;

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
            row.setState("PUBLISHED");
            row.setPublishedAt(OffsetDateTime.now());
        }
    }
}
