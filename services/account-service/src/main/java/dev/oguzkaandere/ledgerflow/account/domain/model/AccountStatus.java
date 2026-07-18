package dev.oguzkaandere.ledgerflow.account.domain.model;

public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED;

    public boolean allowsMutation() {
        return this == ACTIVE;
    }
}
