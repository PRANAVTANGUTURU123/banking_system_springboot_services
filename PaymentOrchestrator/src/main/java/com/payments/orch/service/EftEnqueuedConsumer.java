package com.payments.orch.service;

import com.events.Topics;
import com.events.eft.EftEnqueued;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class EftEnqueuedConsumer {

  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;

  @KafkaListener(topics = Topics.EFT_ENQUEUED, groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, EftEnqueued.class);
    if (processed.existsByHandlerAndEventId("eft-enqueued", evt.eventId())) return;

    Payment p = paymentRepo.findById(evt.paymentId()).orElse(null);
    if (p != null && p.getState().canAdvanceTo(PaymentState.BATCHED)) {
      p.setState(PaymentState.BATCHED);
      p.setBatchId(evt.batchId());
      p.setUpdatedAt(OffsetDateTime.now());
      paymentRepo.save(p);
    }

    processed.save(ProcessedEvent.builder()
        .handler("eft-enqueued")
        .eventId(evt.eventId())
        .processedAt(OffsetDateTime.now())
        .build());
  }
}
