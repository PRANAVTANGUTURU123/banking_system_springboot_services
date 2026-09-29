package com.payments.orch.service;

import com.events.Topics;
import com.events.billpay.BillpayStatusEvent;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class StatusConsumer {
  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;
  private final PaymentFinalizer finalizer;

  @KafkaListener(topics = Topics.BILLPAY_STATUS, groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, BillpayStatusEvent.class);
    String eventId = evt.eventId().toString();
    if (processed.existsByHandlerAndEventId("status", eventId)) return;

    paymentRepo.findById(evt.paymentId()).ifPresentOrElse(
        p -> finalizer.complete(p, "POSTED".equalsIgnoreCase(evt.status()), evt.reason()),
        () -> log.warn("billpay.status for unknown payment {}", evt.paymentId()));

    processed.save(ProcessedEvent.builder()
        .handler("status").eventId(eventId)
        .processedAt(OffsetDateTime.now()).build());
  }
}
