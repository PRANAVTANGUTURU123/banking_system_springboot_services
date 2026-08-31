package com.eft.dto;

import com.eft.model.ExternalAccount;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExternalAccountResponse(
        UUID id,
        String customerId,
        String accountHolderName,
        String institutionNumber,
        String transitNumber,
        String maskedAccountNumber,
        String currency,
        String status,
        OffsetDateTime createdAt
) {
    public static ExternalAccountResponse from(ExternalAccount a) {
        String acct = a.getAccountNumber();
        String masked = "*****" + acct.substring(Math.max(0, acct.length() - 4));
        return new ExternalAccountResponse(
                a.getId(), a.getCustomerId(), a.getAccountHolderName(),
                a.getInstitutionNumber(), a.getTransitNumber(), masked,
                a.getCurrency(), a.getStatus().name(), a.getCreatedAt());
    }
}
