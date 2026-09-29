package com.payments.orch.service;

import com.payments.orch.dto.BillPayRequest;
import com.payments.orch.client.BillerRegistryClient;
import com.commons.exception.BadRequestException;
import com.payments.orch.client.UpstreamErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BillPayValidator {
  private final BillerRegistryClient registry;

  public void validate(BillPayRequest r) {
    if (!UpstreamErrors.call("biller-service", () -> registry.isActive(r.billerReferenceNumber()))) {
      throw new BadRequestException("BILLER_INACTIVE");
    }
    var exec = LocalDate.parse(r.executionDate());
    if (exec.isBefore(LocalDate.now())) {
      throw new BadRequestException("EXECUTION_DATE_PAST");
    }
    if (!"CAD".equals(r.amount().currency())) {
      throw new BadRequestException("CURRENCY_NOT_ALLOWED");
    }
  }
}
