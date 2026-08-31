package com.eftworker.repo;

import com.eftworker.domain.EftBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EftBatchRepository extends JpaRepository<EftBatch, UUID> {
    Optional<EftBatch> findFirstByStatusOrderByCreatedAtAsc(String status);
}
