package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;

public record TransferReference(String value) {
    public static final int MAX_LENGTH = 100;

    public TransferReference {
        value = value == null ? "" : value.trim();
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidTransferException("Reference must contain between 1 and 100 characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
