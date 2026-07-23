package dev.oguzkaandere.ledgerflow.account.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.account.application.event.WorkflowEnvelope;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

class KafkaMdcTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void populatesWorkflowFieldsAndRestoresPreviousContext() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        var payload = new ObjectMapper().valueToTree(Map.of("transferId", transferId.toString()));
        var envelope = new WorkflowEnvelope(
                eventId,
                "ledgerflow.transfer.initiated.v1",
                1,
                Instant.parse("2026-07-23T12:00:00Z"),
                "correlation-123",
                "http-request-123",
                "transfer-service",
                payload);
        MDC.put("existing", "preserved");

        KafkaMdc.run(envelope, () -> {
            assertThat(MDC.get("eventId")).isEqualTo(eventId.toString());
            assertThat(MDC.get("eventType")).isEqualTo("ledgerflow.transfer.initiated.v1");
            assertThat(MDC.get("correlationId")).isEqualTo("correlation-123");
            assertThat(MDC.get("causationId")).isEqualTo("http-request-123");
            assertThat(MDC.get("transferId")).isEqualTo(transferId.toString());
        });

        assertThat(MDC.get("existing")).isEqualTo("preserved");
        assertThat(MDC.get("eventId")).isNull();
        assertThat(MDC.get("correlationId")).isNull();
    }
}
