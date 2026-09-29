package com.payments.orch.service;

import com.payments.orch.client.AccountM2MClient;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import com.payments.orch.repo.PaymentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Moves a payment to its terminal state and settles the funds hold. Shared by
 * both rails' status consumers and the dead-letter consumer.
 *
 * <p>Safe under redelivery: a payment already POSTED/FAILED is left alone, and
 * the AccountService capture/release calls are themselves idempotent, so if the
 * HTTP call succeeds but our DB commit fails, the retried event is harmless.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFinalizer {

  private final AccountM2MClient accounts;
  private final PaymentRepo paymentRepo;

  @Transactional(propagation = Propagation.MANDATORY)
  public void complete(Payment p, boolean posted, String reason) {
    if (p.getState().isTerminal()) {
      log.info("Payment {} already {}, ignoring {} result", p.getPaymentId(), p.getState(), posted ? "POSTED" : "FAILED");
      return;
    }

    if (posted) {
      String ledgerReason = p.getChannel() + " payment " + p.getPaymentId();
      accounts.captureHold(p.getDebtorAccountId(), p.getPaymentId(), ledgerReason);
      p.setState(PaymentState.POSTED);
    } else {
      accounts.releaseHold(p.getDebtorAccountId(), p.getPaymentId());
      p.setState(PaymentState.FAILED);
    }
    p.setReason(reason);
    p.setUpdatedAt(OffsetDateTime.now());
    paymentRepo.save(p);
  }
}
