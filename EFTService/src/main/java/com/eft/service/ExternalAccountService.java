package com.eft.service;

import com.eft.dto.ExternalAccountRequest;
import com.eft.dto.ExternalAccountResponse;
import com.eft.model.ExternalAccount;
import com.eft.model.ExternalAccountStatus;
import com.eft.repository.ExternalAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ExternalAccountService {

    private final ExternalAccountRepository repo;

    public ExternalAccountService(ExternalAccountRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ExternalAccountResponse register(ExternalAccountRequest req) {
        // Idempotent replay: same owner + routing details returns the existing registration
        var existing = repo.findByCustomerIdAndInstitutionNumberAndTransitNumberAndAccountNumber(
                req.customerId(), req.institutionNumber(), req.transitNumber(), req.accountNumber());
        if (existing.isPresent()) {
            return ExternalAccountResponse.from(existing.get());
        }

        ExternalAccount a = new ExternalAccount();
        a.setCustomerId(req.customerId());
        a.setAccountHolderName(req.accountHolderName());
        a.setInstitutionNumber(req.institutionNumber());
        a.setTransitNumber(req.transitNumber());
        a.setAccountNumber(req.accountNumber());
        a.setCurrency(req.currency() == null ? "CAD" : req.currency());
        a.setStatus(ExternalAccountStatus.PENDING_VERIFICATION);
        return ExternalAccountResponse.from(repo.save(a));
    }

    @Transactional
    public ExternalAccountResponse verify(UUID id) {
        ExternalAccount a = require(id);
        if (a.getStatus() == ExternalAccountStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is disabled");
        }
        a.setStatus(ExternalAccountStatus.ACTIVE);
        return ExternalAccountResponse.from(repo.save(a));
    }

    public ExternalAccountResponse get(UUID id) {
        return ExternalAccountResponse.from(require(id));
    }

    public List<ExternalAccountResponse> listByCustomer(String customerId) {
        return repo.findByCustomerId(customerId).stream()
                .map(ExternalAccountResponse::from)
                .toList();
    }

    @Transactional
    public void disable(UUID id) {
        ExternalAccount a = require(id);
        a.setStatus(ExternalAccountStatus.DISABLED);
        repo.save(a);
    }

    public boolean isActive(UUID id) {
        return repo.existsByIdAndStatus(id, ExternalAccountStatus.ACTIVE);
    }

    private ExternalAccount require(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "External account not found: " + id));
    }
}
