package com.payments.orch.service;

import com.account.dto.CreateHoldRequest;
import com.events.eft.EftRequested;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.client.AccountClient;
import com.payments.orch.domain.Outbox;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import com.payments.orch.dto.EftPayRequest;
import com.payments.orch.dto.PaymentAcceptedResponse;
import com.payments.orch.repo.OutboxRepo;
import com.payments.orch.repo.PaymentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * EFT counterpart of {@link BillPayOrchestrator}: validate -> hold funds ->
 * persist Payment -> outbox row on topic eft.requested.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EftOrchestrator {

  private final EftValidator validator;
  private final AccountClient accounts;
  private final PaymentRepo paymentRepo;
  private final OutboxRepo outboxRepo;
  private final ObjectMapper om;

  @Transactional
  public PaymentAcceptedResponse acceptEft(EftPayRequest req, String idemKey) {
    // 1) Idempotency: check if this payment already exists
    var existing = paymentRepo.findByIdempotencyKey(idemKey);
    if (existing.isPresent()) {
      var p = existing.get();
      return new PaymentAcceptedResponse(
          p.getPaymentId(),
          p.getState().name(),
          "/api/v1/payments/" + p.getPaymentId()
      );
    }

    // 2) Business validation (external account active, execution date, currency)
    validator.validate(req);

    // 3) Place hold on the debtor account (paymentId = holdId, same as BillPay)
    var holdReq = new CreateHoldRequest(
            req.amount().value(),
            "EFT",
            null,
            idemKey
    );
    var paymentId = accounts.placeHold(req.debtorAccountId(), idemKey, holdReq).holdId();

    // 4) Persist Payment row in FUNDS_HELD state
    var now = OffsetDateTime.now();
    var payment = Payment.builder()
        .paymentId(paymentId)
        .state(PaymentState.FUNDS_HELD)
        .debtorAccountId(req.debtorAccountId())
        .channel("EFT")
        .externalAccountId(req.externalAccountId())
        .executionDate(LocalDate.parse(req.executionDate()))
        .amountValue(req.amount().value())
        .amountCcy(req.amount().currency())
        .idempotencyKey(idemKey)
        .createdAt(now)
        .updatedAt(now)
        .build();
    paymentRepo.save(payment);

    // 5) Outbox event: eft.requested
    var evt = new EftRequested(
        UUID.randomUUID().toString(),
        paymentId,
        req.debtorAccountId(),
        req.externalAccountId(),
        req.executionDate(),
        req.amount().value(),
        req.amount().currency(),
        now.toString(),
        "1",
        "eft"
    );

    outboxRepo.save(Outbox.builder()
        .topic("eft.requested")
        .key(paymentId)
        .payloadJson(write(evt))
        .state("PENDING")
        .createdAt(now)
        .updatedAt(now)
        .build());

    // 6) Async-202 style response
    return new PaymentAcceptedResponse(
        paymentId,
        PaymentState.FUNDS_HELD.name(),
        "/api/v1/payments/" + paymentId
    );
  }

  private String write(Object o) {
    try {
      return om.writeValueAsString(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
