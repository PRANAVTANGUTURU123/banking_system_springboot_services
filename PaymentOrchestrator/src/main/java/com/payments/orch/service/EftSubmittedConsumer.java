package com.payments.orch.service;

import com.events.eft.EftBatchSubmittedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import com.payments.orch.domain.ProcessedEvent;
import com.payments.orch.repo.PaymentRepo;
import com.payments.orch.repo.ProcessedEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EftSubmittedConsumer {

  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;

  @KafkaListener(topics = "eft.batch.submitted", groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, EftBatchSubmittedEvent.class);

    String eventId = evt.eventId();
    if (eventId == null || eventId.isBlank()) {
      eventId = evt.batchId().toString();
    }

    if (processed.existsByHandlerAndEventId("eft-submitted", eventId)) return;

    List<Payment> list = paymentRepo.findAllByBatchId(evt.batchId());
    var now = OffsetDateTime.now();
    for (var p : list) {
      p.setState(PaymentState.SUBMITTED);
      p.setUpdatedAt(now);
    }
    paymentRepo.saveAll(list);

    processed.save(ProcessedEvent.builder()
        .handler("eft-submitted")
        .eventId(eventId)
        .processedAt(now)
        .build());
  }
}
