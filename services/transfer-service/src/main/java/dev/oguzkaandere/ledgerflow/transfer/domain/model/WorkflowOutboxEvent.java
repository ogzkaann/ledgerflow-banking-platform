package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowOutboxEvent(
        UUID eventId,
        String eventType,
        Transfer transfer,
        Instant occurredAt,
        String causationId,
        Map<String, Object> payload) {
    public WorkflowOutboxEvent {
        if (eventId == null
                || eventType == null
                || eventType.isBlank()
                || transfer == null
                || occurredAt == null
                || causationId == null
                || causationId.isBlank()
                || payload == null) {
            throw new IllegalArgumentException("Workflow outbox event fields are required");
        }
        payload = Map.copyOf(payload);
    }
}
