package dev.oguzkaandere.ledgerflow.transfer.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStatus;
import dev.oguzkaandere.ledgerflow.transfer.support.TransferIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

class TransferWorkflowServiceIT extends TransferIntegrationTest {
    @Autowired
    private TransferApplicationService transfers;

    @Autowired
    private TransferWorkflowService workflow;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void approvedWorkflowReachesCompletedExactlyOnce() {
        Transfer transfer = create("approved-key", "normal");
        workflow.handle(event(TransferWorkflowService.FUNDS_RESERVED, transfer, null));
        workflow.handle(event(TransferWorkflowService.RISK_APPROVED, transfer, null));
        WorkflowEnvelope settled = event(TransferWorkflowService.TRANSFER_SETTLED, transfer, null);
        workflow.handle(settled);
        workflow.handle(settled);

        Transfer completed = transfers.getTransfer(transfer.id());
        assertThat(completed.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfers.getHistory(transfer.id()))
                .extracting(value -> value.toStatus())
                .containsExactly(
                        TransferStatus.PENDING,
                        TransferStatus.FUNDS_RESERVED,
                        TransferStatus.RISK_APPROVED,
                        TransferStatus.SETTLING,
                        TransferStatus.COMPLETED);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_events WHERE event_type=?",
                        Integer.class,
                        TransferWorkflowService.COMPLETED))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void riskRejectionCompensatesBeforeTerminalRejectionAndIgnoresStaleEvents() {
        Transfer transfer = create("rejected-key", "RISK-REJECT");
        workflow.handle(event(TransferWorkflowService.FUNDS_RESERVED, transfer, null));
        workflow.handle(event(TransferWorkflowService.RISK_REJECTED, transfer, "BLOCKED_REFERENCE"));
        assertThat(transfers.getTransfer(transfer.id()).status()).isEqualTo(TransferStatus.COMPENSATING);

        workflow.handle(event(TransferWorkflowService.FUNDS_RELEASED, transfer, null));
        workflow.handle(event(TransferWorkflowService.RISK_APPROVED, transfer, null));

        assertThat(transfers.getTransfer(transfer.id()).status()).isEqualTo(TransferStatus.REJECTED);
        assertThat(transfers.getHistory(transfer.id()))
                .extracting(value -> value.toStatus())
                .containsExactly(
                        TransferStatus.PENDING,
                        TransferStatus.FUNDS_RESERVED,
                        TransferStatus.COMPENSATING,
                        TransferStatus.REJECTED);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_events WHERE event_type=?",
                        Integer.class,
                        TransferWorkflowService.REJECTED))
                .isEqualTo(1);
    }

    @Test
    void reservationRejectionTerminatesWithoutRiskOrSettlement() {
        Transfer transfer = create("insufficient-key", "normal");
        workflow.handle(event(TransferWorkflowService.RESERVATION_REJECTED, transfer, "INSUFFICIENT_FUNDS"));

        assertThat(transfers.getTransfer(transfer.id()).status()).isEqualTo(TransferStatus.REJECTED);
        assertThat(jdbc.queryForObject(
                        "SELECT payload #>> '{payload,reason}' FROM outbox_events WHERE event_type=?",
                        String.class,
                        TransferWorkflowService.REJECTED))
                .isEqualTo("INSUFFICIENT_FUNDS");
    }

    private Transfer create(String key, String reference) {
        return transfers
                .create(new CreateTransferCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new Money(new BigDecimal("125.50"), SupportedCurrency.EUR),
                        new TransferReference(reference),
                        new IdempotencyKey(key),
                        new CorrelationId("correlation-" + key)))
                .transfer();
    }

    private WorkflowEnvelope event(String eventType, Transfer transfer, String reason) {
        Map<String, Object> payload = reason == null
                ? Map.of("transferId", transfer.id().value())
                : Map.of("transferId", transfer.id().value(), "reason", reason);
        return new WorkflowEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                Instant.parse("2026-07-23T12:00:00Z"),
                transfer.correlationId().value(),
                "causing-event",
                eventType.startsWith("ledgerflow.risk") ? "risk-service" : "account-service",
                mapper.valueToTree(payload));
    }
}
