package com.payments.orch.service;

import com.events.Topics;
import com.events.eft.EftStatusEvent;
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
public class EftStatusConsumer {

  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;
  private final PaymentFinalizer finalizer;

  @KafkaListener(topics = Topics.EFT_STATUS, groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, EftStatusEvent.class);
    String eventId = evt.eventId().toString();
    if (processed.existsByHandlerAndEventId("eft-status", eventId)) return;

    paymentRepo.findById(evt.paymentId()).ifPresentOrElse(
        p -> finalizer.complete(p, "POSTED".equalsIgnoreCase(evt.status()), evt.reason()),
        () -> log.warn("eft.status for unknown payment {}", evt.paymentId()));

    processed.save(ProcessedEvent.builder()
        .handler("eft-status")
        .eventId(eventId)
        .processedAt(OffsetDateTime.now())
        .build());
  }
}
