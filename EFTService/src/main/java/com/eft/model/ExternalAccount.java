package com.eft.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "external_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_extacct_owner_routing",
                        columnNames = {"customer_id", "institution_number", "transit_number", "account_number"})
        },
        indexes = {
                @Index(name = "idx_extacct_customer", columnList = "customer_id")
        }
)
public class ExternalAccount {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "account_holder_name", nullable = false, length = 128)
    private String accountHolderName;

    @Column(name = "institution_number", nullable = false, length = 3)
    private String institutionNumber;

    @Column(name = "transit_number", nullable = false, length = 5)
    private String transitNumber;

    @Column(name = "account_number", nullable = false, length = 12)
    private String accountNumber;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ExternalAccountStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getInstitutionNumber() { return institutionNumber; }
    public void setInstitutionNumber(String institutionNumber) { this.institutionNumber = institutionNumber; }
    public String getTransitNumber() { return transitNumber; }
    public void setTransitNumber(String transitNumber) { this.transitNumber = transitNumber; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public ExternalAccountStatus getStatus() { return status; }
    public void setStatus(ExternalAccountStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
