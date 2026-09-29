package com.billpay.worker.repo;

import com.billpay.worker.domain.Batch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    /**
     * The OPEN batch, row-locked. The Kafka listener (adding lines) and the
     * age-cutoff scheduler (closing batches) both take this lock, so a line can
     * never be added to a batch that is being closed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Batch> findFirstByStatusOrderByCreatedAtAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Batch> findByStatusAndCreatedAtBefore(String status, OffsetDateTime cutoff);
}
