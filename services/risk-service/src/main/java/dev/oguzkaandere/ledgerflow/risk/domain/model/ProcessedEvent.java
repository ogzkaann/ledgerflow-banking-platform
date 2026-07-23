package dev.oguzkaandere.ledgerflow.risk.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProcessedEvent(UUID eventId, String eventType, Instant processedAt) {
    public ProcessedEvent {
        if (eventId == null || eventType == null || eventType.isBlank() || processedAt == null) {
            throw new IllegalArgumentException("Processed event fields are required");
        }
    }
}
