package dev.oguzkaandere.ledgerflow.account.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID ledgerEntryId,
        AccountId accountId,
        LedgerEntryType type,
        Money amount,
        LedgerReference reference,
        Instant createdAt) {

    public LedgerEntry {
        Objects.requireNonNull(ledgerEntryId, "Ledger entry ID is required");
        Objects.requireNonNull(accountId, "Account ID is required");
        Objects.requireNonNull(type, "Ledger entry type is required");
        Objects.requireNonNull(amount, "Ledger amount is required");
        Objects.requireNonNull(reference, "Ledger reference is required");
        Objects.requireNonNull(createdAt, "Ledger entry creation time is required");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be greater than zero");
        }
    }

    public static LedgerEntry credit(
            UUID ledgerEntryId, AccountId accountId, Money amount, LedgerReference reference, Instant createdAt) {
        return new LedgerEntry(ledgerEntryId, accountId, LedgerEntryType.CREDIT, amount, reference, createdAt);
    }

    public static LedgerEntry debit(
            UUID ledgerEntryId, AccountId accountId, Money amount, LedgerReference reference, Instant createdAt) {
        return new LedgerEntry(ledgerEntryId, accountId, LedgerEntryType.DEBIT, amount, reference, createdAt);
    }
}
