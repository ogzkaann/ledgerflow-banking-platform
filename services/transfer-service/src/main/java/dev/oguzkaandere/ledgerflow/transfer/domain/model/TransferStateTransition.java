package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TransferStateTransition(
        UUID id,
        TransferId transferId,
        TransferStatus fromStatus,
        TransferStatus toStatus,
        String reason,
        Instant occurredAt,
        long sequence) {
    public TransferStateTransition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(toStatus);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(occurredAt);
        if (sequence < 0) {
            throw new IllegalArgumentException("Transition sequence cannot be negative");
        }
    }
}
