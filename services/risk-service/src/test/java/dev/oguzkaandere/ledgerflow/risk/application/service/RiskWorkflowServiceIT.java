package dev.oguzkaandere.ledgerflow.risk.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.risk.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.risk.support.RiskIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import tools.jackson.databind.ObjectMapper;

class RiskWorkflowServiceIT extends RiskIntegrationTest {
    @Autowired
    private RiskWorkflowService workflow;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void persistsOneDeterministicDecisionAndOutboxForDuplicateEvent() {
        WorkflowEnvelope envelope = reserved("125.50", "normal-reference");
        workflow.handle(envelope);
        workflow.handle(envelope);

        assertThat(jdbc.queryForObject("SELECT outcome FROM risk_decisions", String.class))
                .isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT rule_version FROM risk_decisions", String.class))
                .isEqualTo("risk-rules-v1");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_decisions", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void blockedMarkerProducesRejection() {
        workflow.handle(reserved("125.50", "invoice-RISK-REJECT"));

        assertThat(jdbc.queryForObject("SELECT outcome FROM risk_decisions", String.class))
                .isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("SELECT reason FROM risk_decisions", String.class))
                .isEqualTo("BLOCKED_REFERENCE");
        assertThat(jdbc.queryForObject("SELECT event_type FROM outbox_events", String.class))
                .isEqualTo("ledgerflow.risk.rejected.v1");
    }

    @Test
    void outboxFailureRollsBackDecisionAndProcessedEvent() {
        jdbc.execute("""
                CREATE FUNCTION reject_risk_outbox() RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'controlled risk outbox failure';
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbc.execute("""
                CREATE TRIGGER reject_risk_outbox_trigger BEFORE INSERT ON outbox_events
                FOR EACH ROW EXECUTE FUNCTION reject_risk_outbox()
                """);
        try {
            assertThatThrownBy(() -> workflow.handle(reserved("125.50", "normal")))
                    .isInstanceOf(DataAccessException.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_decisions", Integer.class))
                    .isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class))
                    .isZero();
        } finally {
            jdbc.execute("DROP TRIGGER reject_risk_outbox_trigger ON outbox_events");
            jdbc.execute("DROP FUNCTION reject_risk_outbox()");
        }
    }

    private WorkflowEnvelope reserved(String amount, String reference) {
        return new WorkflowEnvelope(
                UUID.randomUUID(),
                "ledgerflow.account.funds-reserved.v1",
                1,
                Instant.parse("2026-07-23T12:00:00Z"),
                "correlation-risk",
                "account-event",
                "account-service",
                mapper.valueToTree(Map.of(
                        "transferId", UUID.randomUUID(),
                        "reservationId", UUID.randomUUID(),
                        "sourceAccountId", UUID.randomUUID(),
                        "destinationAccountId", UUID.randomUUID(),
                        "amount", amount,
                        "currency", "EUR",
                        "reference", reference)));
    }
}
