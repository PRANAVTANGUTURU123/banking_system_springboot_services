package com.settlement.repo;

import com.settlement.domain.EftBatchSettlement;
import com.settlement.domain.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EftBatchSettlementRepository extends JpaRepository<EftBatchSettlement, Long> {

    Optional<EftBatchSettlement> findByBatchId(UUID batchId);

    List<EftBatchSettlement> findByStatusAndNextRetryAtLessThanEqual(SettlementStatus status, OffsetDateTime now);
}
