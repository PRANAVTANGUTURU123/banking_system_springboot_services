package com.events.eft;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by PaymentOrchestrator (via outbox) on topic eft.requested. */
public record EftRequested(
        String eventId,
        UUID paymentId,
        UUID debtorAccountId,
        UUID externalAccountId,
        String executionDate,      // yyyy-MM-dd
        BigDecimal amountValue,
        String amountCcy,
        String occurredAt,
        String schemaVersion,
        String channel             // "eft"
) {}
