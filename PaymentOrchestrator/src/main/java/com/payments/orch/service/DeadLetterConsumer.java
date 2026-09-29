package com.payments.orch.service;

import com.events.BatchDeadLettered;
import com.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.domain.ProcessedEvent;
import com.payments.orch.repo.PaymentRepo;
import com.payments.orch.repo.ProcessedEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * A batch that settlement gave up on (retries exhausted) would otherwise leave
 * its payments stuck in BATCHED with funds held. Fail them and release the holds.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;
  private final PaymentFinalizer finalizer;

  @KafkaListener(topics = {Topics.BILL_BATCH_DLQ, Topics.EFT_BATCH_DLQ}, groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, BatchDeadLettered.class);
    if (processed.existsByHandlerAndEventId("batch-dlq", evt.eventId())) return;

    var payments = paymentRepo.findAllByBatchId(evt.batchId());
    log.warn("Batch {} dead-lettered after {} attempts; failing {} payments",
        evt.batchId(), evt.attempts(), payments.size());
    for (var p : payments) {
      finalizer.complete(p, false, "BATCH_DEAD_LETTERED: " + evt.reason());
    }

    processed.save(ProcessedEvent.builder()
        .handler("batch-dlq")
        .eventId(evt.eventId())
        .processedAt(OffsetDateTime.now())
        .build());
  }
}
