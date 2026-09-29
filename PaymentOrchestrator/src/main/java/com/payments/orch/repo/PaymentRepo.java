package com.payments.orch.repo;

import com.payments.orch.domain.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, UUID> {
  Optional<Payment> findByIdempotencyKey(String idemKey);
  List<Payment> findAllByBatchId(UUID batchId);
}
