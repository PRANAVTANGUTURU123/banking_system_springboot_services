package com.eft.repository;

import com.eft.model.ExternalAccount;
import com.eft.model.ExternalAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, UUID> {

    List<ExternalAccount> findByCustomerId(String customerId);

    Optional<ExternalAccount> findByCustomerIdAndInstitutionNumberAndTransitNumberAndAccountNumber(
            String customerId, String institutionNumber, String transitNumber, String accountNumber);

    boolean existsByIdAndStatus(UUID id, ExternalAccountStatus status);
}
