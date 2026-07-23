package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;

public record CorrelationId(String value) {
    public static final int MAX_LENGTH = 100;

    public CorrelationId {
        value = value == null ? "" : value.trim();
        if (value.isBlank() || value.length() > MAX_LENGTH || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new InvalidTransferException(
                    "Correlation ID must be 1-100 log-safe letters, numbers, dots, underscores, colons, or hyphens");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
