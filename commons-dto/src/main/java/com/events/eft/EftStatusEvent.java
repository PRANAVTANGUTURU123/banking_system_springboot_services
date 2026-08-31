package com.events.eft;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Published by SettlementService on topic eft.status, one per payment, derived from the network ack. */
public record EftStatusEvent(
        UUID eventId,
        UUID paymentId,
        UUID batchId,
        String status,             // "POSTED" / "FAILED"
        String reason,
        OffsetDateTime updatedAt
) {}
