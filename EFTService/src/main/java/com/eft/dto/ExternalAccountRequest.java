package com.eft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExternalAccountRequest(
        @NotBlank String customerId,
        @NotBlank String accountHolderName,
        @NotBlank @Pattern(regexp = "^\\d{3}$", message = "institutionNumber must be 3 digits") String institutionNumber,
        @NotBlank @Pattern(regexp = "^\\d{5}$", message = "transitNumber must be 5 digits") String transitNumber,
        @NotBlank @Pattern(regexp = "^\\d{7,12}$", message = "accountNumber must be 7-12 digits") String accountNumber,
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be ISO-4217 (e.g. CAD)") String currency
) {}
