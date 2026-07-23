package dev.oguzkaandere.ledgerflow.transfer.application.event;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record WorkflowEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        JsonNode payload) {
    public WorkflowEnvelope {
        if (eventId == null
                || eventType == null
                || eventType.isBlank()
                || eventVersion != 1
                || occurredAt == null
                || correlationId == null
                || correlationId.isBlank()
                || causationId == null
                || causationId.isBlank()
                || producer == null
                || producer.isBlank()
                || payload == null
                || !payload.isObject()) {
            throw new IllegalArgumentException("Malformed or unsupported workflow event envelope");
        }
    }
}
