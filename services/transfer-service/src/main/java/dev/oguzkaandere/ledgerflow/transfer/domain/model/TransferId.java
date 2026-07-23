package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TransferId(UUID value) {
    public TransferId {
        Objects.requireNonNull(value, "transfer ID is required");
    }

    public static TransferId from(UUID value) {
        return new TransferId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
