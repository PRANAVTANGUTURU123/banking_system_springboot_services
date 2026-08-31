package com.payments.orch.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record EftPayRequest(
  @NotNull UUID debtorAccountId,
  @NotNull UUID externalAccountId,
  @NotBlank String executionDate,          // yyyy-MM-dd
  @NotNull  AmountDto amount,
  String note
) {}
