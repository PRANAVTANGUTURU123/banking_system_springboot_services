package com.settlement.repo;

import com.settlement.domain.EftBatchSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EftBatchSettlementRepository extends JpaRepository<EftBatchSettlement, Long> {
    Optional<EftBatchSettlement> findByBatchId(UUID batchId);
}
