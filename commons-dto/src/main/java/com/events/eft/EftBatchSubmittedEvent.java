package com.events.eft;

import java.util.UUID;

/** Published by SettlementService on topic eft.batch.submitted after the (mock) network upload. */
public record EftBatchSubmittedEvent(
        String eventId,
        UUID batchId,
        String networkReference
) {}
