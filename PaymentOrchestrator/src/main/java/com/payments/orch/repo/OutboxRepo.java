package com.payments.orch.repo;

import com.payments.orch.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxRepo extends JpaRepository<Outbox, Long> {

  /**
   * Oldest pending rows, row-locked. SKIP LOCKED lets several orchestrator
   * instances run the publisher without sending the same row twice.
   */
  @Query(value = "select * from outbox where state = 'PENDING' order by id limit 200 for update skip locked",
         nativeQuery = true)
  List<Outbox> lockPendingBatch();
}
