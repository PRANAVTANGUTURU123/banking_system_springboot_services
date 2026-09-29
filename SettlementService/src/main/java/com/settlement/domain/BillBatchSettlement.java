package com.settlement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bill_batch_settlement")
@Getter
@Setter
public class BillBatchSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;
    
    
    private int retryCount;   // failed upload attempts so far

    private OffsetDateTime nextRetryAt; // when the retry scheduler picks it up again (status FAILED)


    private String pain001FileName;

    private String centralReference;

    private String lastError;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
