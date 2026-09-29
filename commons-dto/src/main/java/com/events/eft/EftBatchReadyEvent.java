package com.events.eft;

import java.util.UUID;

/** Published by EFTWorkerService on topic eft.batch.ready when a batch is closed (size or age cutoff). */
public record EftBatchReadyEvent(
        String eventId,
        UUID batchId,
        int lineCount,
        String occurredAt,
        String schemaVersion,
        String channel
) {}
