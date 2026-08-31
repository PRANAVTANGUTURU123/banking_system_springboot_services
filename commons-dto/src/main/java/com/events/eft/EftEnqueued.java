package com.events.eft;

import java.util.UUID;

/** Published by EFTWorkerService on topic eft.enqueued once a payment is filed into a batch. */
public record EftEnqueued(
        String eventId,
        UUID paymentId,
        UUID batchId,
        String occurredAt,
        String schemaVersion,
        String channel
) {}
