package com.events;

/**
 * Single source of truth for Kafka topic names shared across services.
 * Constants (not config) so {@code @KafkaListener(topics = ...)} can use them
 * and producer/consumer names can never drift apart.
 */
public final class Topics {

    private Topics() {}

    // ---- Bill Pay rail ----
    public static final String BILLPAY_REQUESTED    = "billpay.requested";    // orchestrator -> worker (outbox)
    public static final String BILLPAY_ENQUEUED     = "billpay.enqueued";     // worker -> orchestrator
    public static final String BILL_BATCH_READY     = "bill.batch.ready";     // worker -> settlement
    public static final String BILL_BATCH_SUBMITTED = "bill.batch.submitted"; // settlement -> orchestrator
    public static final String BILLPAY_STATUS       = "billpay.status";       // settlement -> orchestrator
    public static final String BILL_BATCH_DLQ       = "bill.batch.dlq";       // settlement -> orchestrator (retries exhausted)
    public static final String CENTRAL1_PAIN002     = "central1.pain002";     // clearing mock -> settlement

    // ---- EFT rail ----
    public static final String EFT_REQUESTED        = "eft.requested";
    public static final String EFT_ENQUEUED         = "eft.enqueued";
    public static final String EFT_BATCH_READY      = "eft.batch.ready";
    public static final String EFT_BATCH_SUBMITTED  = "eft.batch.submitted";
    public static final String EFT_STATUS           = "eft.status";
    public static final String EFT_BATCH_DLQ        = "eft.batch.dlq";
    public static final String EFTNETWORK_ACK       = "eftnetwork.ack";
}
