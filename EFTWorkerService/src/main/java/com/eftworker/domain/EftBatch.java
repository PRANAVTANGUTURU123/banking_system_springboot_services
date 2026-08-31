package com.eftworker.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "eft_batches")
public class EftBatch {

    @Id
    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // OPEN, READY_EMITTED

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public EftBatch() {}

    public EftBatch(UUID batchId, String status, OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
