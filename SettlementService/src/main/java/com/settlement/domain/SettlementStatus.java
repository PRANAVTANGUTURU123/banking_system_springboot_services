package com.settlement.domain;

public enum SettlementStatus {
    READY,
    FILE_BUILT,
    UPLOADED,
    SUBMITTED,
    FAILED,         // last upload attempt failed; retried at nextRetryAt
    DEAD_LETTERED   // retries exhausted; parked on the DLQ topic
}
