package dev.oguzkaandere.ledgerflow.account.domain.model;

public record LedgerReference(String value) {

    public static final int MAX_LENGTH = 100;

    public LedgerReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ledger reference must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Ledger reference must not exceed " + MAX_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
