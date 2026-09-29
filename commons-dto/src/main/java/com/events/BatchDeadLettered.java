package com.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Published by SettlementService on {@link Topics#BILL_BATCH_DLQ} / {@link Topics#EFT_BATCH_DLQ}
 * when a batch could not be submitted after all retries. The orchestrator fails
 * every payment in the batch and releases its funds hold.
 */
public record BatchDeadLettered(
        String eventId,
        UUID batchId,
        int attempts,
        String reason,
        OffsetDateTime occurredAt
) {}
