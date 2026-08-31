package com.events.eft;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Published by the EFT network mock on topic eftnetwork.ack; consumed by SettlementService. */
public record EftAckFileMessage(
        UUID batchId,
        List<EftAckMessage> items,
        OffsetDateTime generatedAt
) {}
