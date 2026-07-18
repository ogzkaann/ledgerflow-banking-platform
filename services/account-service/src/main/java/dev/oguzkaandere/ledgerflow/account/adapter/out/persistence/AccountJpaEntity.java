package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountStatus;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
class AccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_reference", nullable = false, length = 100)
    private String ownerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private SupportedCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "available_balance", nullable = false, precision = Money.PRECISION, scale = Money.SCALE)
    private BigDecimal availableBalance;

    @Column(name = "reserved_balance", nullable = false, precision = Money.PRECISION, scale = Money.SCALE)
    private BigDecimal reservedBalance;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountJpaEntity() {}

    private AccountJpaEntity(Account account) {
        id = account.id().value();
        updateFrom(account);
        version = account.version();
        createdAt = account.createdAt();
    }

    static AccountJpaEntity fromDomain(Account account) {
        return new AccountJpaEntity(account);
    }

    void updateFrom(Account account) {
        ownerReference = account.ownerReference();
        currency = account.currency();
        status = account.status();
        availableBalance = account.availableBalance().amount();
        reservedBalance = account.reservedBalance().amount();
        updatedAt = account.updatedAt();
    }

    UUID id() {
        return id;
    }

    Account toDomain() {
        return new Account(
                AccountId.from(id),
                ownerReference,
                currency,
                status,
                new Money(availableBalance, currency),
                new Money(reservedBalance, currency),
                version,
                createdAt,
                updatedAt);
    }
}
