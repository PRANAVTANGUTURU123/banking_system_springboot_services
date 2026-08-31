package com.eftworker.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "eft_batch_lines",
        indexes = {@Index(name = "idx_eft_line_batch", columnList = "batch_id")}
)
public class EftBatchLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public EftBatchLine() {}

    public EftBatchLine(UUID batchId, UUID paymentId, int lineNo, OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.paymentId = paymentId;
        this.lineNo = lineNo;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
