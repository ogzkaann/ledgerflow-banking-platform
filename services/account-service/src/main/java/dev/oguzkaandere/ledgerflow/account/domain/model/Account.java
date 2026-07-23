package dev.oguzkaandere.ledgerflow.account.domain.model;

import dev.oguzkaandere.ledgerflow.account.domain.exception.AccountStateException;
import java.time.Instant;
import java.util.Objects;

public record Account(
        AccountId id,
        String ownerReference,
        SupportedCurrency currency,
        AccountStatus status,
        Money availableBalance,
        Money reservedBalance,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static final int MAX_OWNER_REFERENCE_LENGTH = 100;

    public Account {
        Objects.requireNonNull(id, "Account ID is required");
        if (ownerReference == null || ownerReference.isBlank()) {
            throw new IllegalArgumentException("Owner reference must not be blank");
        }
        ownerReference = ownerReference.trim();
        if (ownerReference.length() > MAX_OWNER_REFERENCE_LENGTH) {
            throw new IllegalArgumentException(
                    "Owner reference must not exceed " + MAX_OWNER_REFERENCE_LENGTH + " characters");
        }
        Objects.requireNonNull(currency, "Account currency is required");
        Objects.requireNonNull(status, "Account status is required");
        Objects.requireNonNull(availableBalance, "Available balance is required");
        Objects.requireNonNull(reservedBalance, "Reserved balance is required");
        availableBalance.requireSameCurrency(reservedBalance);
        if (availableBalance.currency() != currency) {
            throw new IllegalArgumentException("Available balance currency must match the account currency");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Account version must not be negative");
        }
        Objects.requireNonNull(createdAt, "Account creation time is required");
        Objects.requireNonNull(updatedAt, "Account update time is required");
    }

    public static Account create(
            AccountId accountId, String ownerReference, SupportedCurrency currency, Instant createdAt) {
        Money zero = Money.zero(currency);
        return new Account(
                accountId, ownerReference, currency, AccountStatus.ACTIVE, zero, zero, 0, createdAt, createdAt);
    }

    public Account credit(Money amount, Instant updatedAt) {
        if (!status.allowsMutation()) {
            throw new AccountStateException(id, status);
        }
        availableBalance.requireSameCurrency(amount);
        return new Account(
                id,
                ownerReference,
                currency,
                status,
                availableBalance.add(amount),
                reservedBalance,
                version,
                createdAt,
                updatedAt);
    }

    public Account reserve(Money amount, Instant updatedAt) {
        requireMutable(amount);
        return new Account(
                id,
                ownerReference,
                currency,
                status,
                availableBalance.subtract(amount),
                reservedBalance.add(amount),
                version,
                createdAt,
                updatedAt);
    }

    public Account release(Money amount, Instant updatedAt) {
        requireMutable(amount);
        return new Account(
                id,
                ownerReference,
                currency,
                status,
                availableBalance.add(amount),
                reservedBalance.subtract(amount),
                version,
                createdAt,
                updatedAt);
    }

    public Account settleReserved(Money amount, Instant updatedAt) {
        requireMutable(amount);
        return new Account(
                id,
                ownerReference,
                currency,
                status,
                availableBalance,
                reservedBalance.subtract(amount),
                version,
                createdAt,
                updatedAt);
    }

    private void requireMutable(Money amount) {
        if (!status.allowsMutation()) {
            throw new AccountStateException(id, status);
        }
        availableBalance.requireSameCurrency(amount);
    }
}
