package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntryType;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
class LedgerEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType type;

    @Column(nullable = false, precision = Money.PRECISION, scale = Money.SCALE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private SupportedCurrency currency;

    @Column(nullable = false, length = 100)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntryJpaEntity() {}

    private LedgerEntryJpaEntity(LedgerEntry entry) {
        id = entry.ledgerEntryId();
        accountId = entry.accountId().value();
        type = entry.type();
        amount = entry.amount().amount();
        currency = entry.amount().currency();
        reference = entry.reference().value();
        createdAt = entry.createdAt();
    }

    static LedgerEntryJpaEntity fromDomain(LedgerEntry entry) {
        return new LedgerEntryJpaEntity(entry);
    }

    LedgerEntry toDomain() {
        return new LedgerEntry(
                id,
                AccountId.from(accountId),
                type,
                Money.positive(amount, currency),
                new LedgerReference(reference),
                createdAt);
    }
}
