package com.events.eft;

import java.util.UUID;

/** Published by EFTWorkerService on topic eft.batch.ready when a batch reaches its threshold. */
public record EftBatchReadyEvent(
        String eventId,
        UUID batchId,
        String occurredAt,
        String schemaVersion,
        String channel
) {}
