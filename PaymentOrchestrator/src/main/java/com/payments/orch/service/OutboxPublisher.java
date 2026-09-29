package com.payments.orch.service;

import com.payments.orch.domain.Outbox;
import com.payments.orch.repo.OutboxRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

  private final OutboxRepo outboxRepo;
  private final KafkaTemplate<String,String> kafka;

  @Scheduled(fixedDelayString = "${outbox.publish.fixedDelayMs:1000}")
  @Transactional
  public void publish() {
    List<Outbox> batch = outboxRepo.lockPendingBatch();
    for (var row: batch) {
      try {
        kafka.send(row.getTopic(), row.getKey().toString(), row.getPayloadJson()).get();
      } catch (Exception e) {
        // Leave the row PENDING so the next run retries it, and stop here so
        // later events are not published ahead of this one.
        log.warn("Outbox publish failed id={} topic={}, will retry: {}", row.getId(), row.getTopic(), e.getMessage());
        break;
      }
      row.setState("PUBLISHED");
      row.setUpdatedAt(OffsetDateTime.now());
    }
  }
}
