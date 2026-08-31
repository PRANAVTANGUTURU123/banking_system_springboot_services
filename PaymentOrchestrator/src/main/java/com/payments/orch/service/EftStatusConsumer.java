package com.payments.orch.service;

import com.account.dto.PostingRequest;
import com.events.eft.EftStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.client.AccountM2MClient;
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
public class EftStatusConsumer {

  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;
  private final AccountM2MClient accountM2MClient;

  @KafkaListener(topics = "eft.status", groupId = "payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, EftStatusEvent.class);
    if (processed.existsByHandlerAndEventId("eft-status", evt.eventId().toString())) return;

    Payment p = paymentRepo.findById(evt.paymentId()).orElse(null);
    if (p == null) return;

    if ("POSTED".equalsIgnoreCase(evt.status())) {
      PostingRequest r = new PostingRequest(p.getAmountValue(), evt.reason());
      accountM2MClient.releaseHold(p.getDebtorAccountId(), p.getPaymentId());
      accountM2MClient.debit(p.getDebtorAccountId(), null, r);
      p.setState(PaymentState.POSTED);
    } else {
      accountM2MClient.releaseHold(p.getDebtorAccountId(), p.getPaymentId());
      p.setState(PaymentState.FAILED);
    }
    p.setReason(evt.reason());
    p.setUpdatedAt(OffsetDateTime.now());
    paymentRepo.save(p);

    processed.save(ProcessedEvent.builder()
        .handler("eft-status")
        .eventId(evt.eventId().toString())
        .processedAt(OffsetDateTime.now())
        .build());
  }
}
