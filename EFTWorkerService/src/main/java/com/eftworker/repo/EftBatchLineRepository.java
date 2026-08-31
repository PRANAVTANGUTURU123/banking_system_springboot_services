package com.eftworker.repo;

import com.eftworker.domain.EftBatchLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EftBatchLineRepository extends JpaRepository<EftBatchLine, Long> {
    long countByBatchId(UUID batchId);
    List<EftBatchLine> findAllByBatchId(UUID batchId);
}
