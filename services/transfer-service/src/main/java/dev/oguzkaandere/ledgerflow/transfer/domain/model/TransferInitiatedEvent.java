package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TransferInitiatedEvent(UUID eventId, Transfer transfer, Instant occurredAt, String causationId) {
    public static final String EVENT_TYPE = "ledgerflow.transfer.initiated.v1";
    public static final int EVENT_VERSION = 1;

    public TransferInitiatedEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(transfer);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(causationId);
    }
}
