package com.payments.orch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import com.payments.orch.domain.ProcessedEvent;
import com.events.Topics;
import com.events.billpay.*;
import com.payments.orch.repo.PaymentRepo;
import com.payments.orch.repo.ProcessedEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmittedConsumer {
  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;

  @KafkaListener(topics = Topics.BILL_BATCH_SUBMITTED, groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, BillBatchSubmitted.class);

    // A batch is submitted once, so the batchId identifies the event when no eventId is sent
    String eventId = evt.getEventId();
    if (eventId == null || eventId.isBlank()) {
      eventId = evt.getBatchId();
    }

    if (processed.existsByHandlerAndEventId("submitted", eventId)) {
      return;
    }

    List<Payment> list = paymentRepo.findAllByBatchId(UUID.fromString(evt.getBatchId()));
    var now = OffsetDateTime.now();
    for (var p : list) {
      if (p.getState().canAdvanceTo(PaymentState.SUBMITTED)) {
        p.setState(PaymentState.SUBMITTED);
        p.setUpdatedAt(now);
      }
    }
    paymentRepo.saveAll(list);

    processed.save(ProcessedEvent.builder()
        .handler("submitted")
        .eventId(eventId)
        .processedAt(now)
        .build());
  }

}
