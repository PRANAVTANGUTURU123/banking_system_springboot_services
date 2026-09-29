package com.account.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account.model.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * SELECT ... FOR UPDATE on the account row. Every balance/hold mutation goes
     * through this so concurrent debits and holds on one account are serialized
     * and can't both pass the available-funds check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    List<Account> findByCustomerId(String customerId);
    Optional<Account> findByRequestFingerprint(String fingerprint);
    Optional<Account> findByAccountNumber(String accountNumber);
}
