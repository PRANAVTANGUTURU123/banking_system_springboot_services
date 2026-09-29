package com.eftworker.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Oldest pending rows, row-locked; SKIP LOCKED keeps multiple instances from double-sending. */
    @Query(value = "select * from outbox_event where state = 'PENDING' order by id limit 200 for update skip locked",
           nativeQuery = true)
    List<OutboxEvent> lockPendingBatch();
}
