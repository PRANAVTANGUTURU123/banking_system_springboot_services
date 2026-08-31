package com.events.eft;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One per-payment acknowledgement item inside an {@link EftAckFileMessage}. Mirrors Pain002Message. */
public record EftAckMessage(
        UUID paymentId,
        UUID batchId,
        String endToEndId,
        String statusCode,         // e.g. ACSP / RJCT
        String statusLabel,
        boolean success,
        String reason,
        OffsetDateTime timestamp
) {}
