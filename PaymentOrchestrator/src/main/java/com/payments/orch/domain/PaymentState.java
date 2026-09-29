package com.payments.orch.domain;

/** Declared in lifecycle order; transitions only ever move forward. */
public enum PaymentState {
  FUNDS_HELD, BATCHED, SUBMITTED, POSTED, FAILED;

  public boolean isTerminal() {
    return this == POSTED || this == FAILED;
  }

  /**
   * Guards against out-of-order or redelivered events: e.g. a late
   * billpay.enqueued must not move a SUBMITTED payment back to BATCHED.
   */
  public boolean canAdvanceTo(PaymentState next) {
    return !isTerminal() && next.ordinal() > ordinal();
  }
}
