package com.eftworker.repo;

import com.eftworker.domain.EftBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EftBatchRepository extends JpaRepository<EftBatch, UUID> {

    /** Row-locked so the listener (adding lines) and the age-cutoff scheduler (closing) can't interleave. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EftBatch> findFirstByStatusOrderByCreatedAtAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EftBatch> findByStatusAndCreatedAtBefore(String status, OffsetDateTime cutoff);
}
