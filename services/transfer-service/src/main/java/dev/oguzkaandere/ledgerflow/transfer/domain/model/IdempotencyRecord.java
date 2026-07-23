package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IdempotencyRecord(
        UUID id,
        String scope,
        IdempotencyKey key,
        String fingerprint,
        TransferId transferId,
        int httpStatus,
        Instant createdAt) {
    public static final String CREATE_TRANSFER_SCOPE = "CREATE_TRANSFER";

    public IdempotencyRecord {
        Objects.requireNonNull(id);
        Objects.requireNonNull(scope);
        Objects.requireNonNull(key);
        Objects.requireNonNull(fingerprint);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(createdAt);
    }
}
