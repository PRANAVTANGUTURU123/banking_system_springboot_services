package com.settlement.repo;

import com.settlement.domain.BillBatchSettlement;
import com.settlement.domain.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillBatchSettlementRepository extends JpaRepository<BillBatchSettlement, Long> {

    Optional<BillBatchSettlement> findByBatchId(UUID batchId);

    List<BillBatchSettlement> findByStatusAndNextRetryAtLessThanEqual(SettlementStatus status, OffsetDateTime now);
}
