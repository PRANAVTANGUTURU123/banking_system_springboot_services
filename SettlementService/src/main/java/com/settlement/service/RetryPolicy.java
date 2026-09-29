package com.settlement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

/** Exponential backoff for batch uploads: base, 2x base, 4x base, ... then dead-letter. */
@Component
public class RetryPolicy {

    private final int maxRetries;
    private final Duration baseBackoff;

    public RetryPolicy(@Value("${settlement.retry.max-retries:3}") int maxRetries,
                       @Value("${settlement.retry.base-backoff:5s}") Duration baseBackoff) {
        this.maxRetries = maxRetries;
        this.baseBackoff = baseBackoff;
    }

    /** @param failedAttempts attempts that have failed so far, including the one just now */
    public boolean exhausted(int failedAttempts) {
        return failedAttempts > maxRetries;
    }

    public OffsetDateTime nextAttemptAt(int failedAttempts) {
        long multiplier = 1L << Math.min(failedAttempts - 1, 20);
        return OffsetDateTime.now().plus(baseBackoff.multipliedBy(multiplier));
    }
}
