package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;

public record IdempotencyKey(String value) {
    public static final int MAX_LENGTH = 100;

    public IdempotencyKey {
        value = value == null ? "" : value.trim();
        if (value.isBlank() || value.length() > MAX_LENGTH || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new InvalidTransferException(
                    "Idempotency-Key must be 1-100 letters, numbers, dots, underscores, colons, or hyphens");
        }
    }

    public String safeLogToken() {
        return Integer.toUnsignedString(value.hashCode(), 16);
    }

    @Override
    public String toString() {
        return value;
    }
}
