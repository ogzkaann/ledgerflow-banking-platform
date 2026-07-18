package dev.oguzkaandere.ledgerflow.account.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "Account ID is required");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId from(UUID value) {
        return new AccountId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
