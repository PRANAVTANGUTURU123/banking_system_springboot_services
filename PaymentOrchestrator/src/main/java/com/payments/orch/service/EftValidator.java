package com.payments.orch.service;

import com.payments.orch.client.EftAccountClient;
import com.payments.orch.dto.EftPayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class EftValidator {
  private final EftAccountClient registry;

  public void validate(EftPayRequest r) {
    if (!registry.isActive(r.externalAccountId())) {
      throw new IllegalArgumentException("EXTERNAL_ACCOUNT_INACTIVE");
    }
    var exec = LocalDate.parse(r.executionDate());
    if (exec.isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("EXECUTION_DATE_PAST");
    }
    if (!"CAD".equals(r.amount().currency())) {
      throw new IllegalArgumentException("CURRENCY_NOT_ALLOWED");
    }
  }
}
