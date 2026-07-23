package dev.oguzkaandere.ledgerflow.transfer.application.service;

import dev.oguzkaandere.ledgerflow.transfer.adapter.out.eventing.TransferProcessedEventStore;
import dev.oguzkaandere.ledgerflow.transfer.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.TransferNotFoundException;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStatus;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.WorkflowOutboxEvent;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.OutboxRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.TransferHistoryRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.TransferRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferWorkflowService {
    public static final String FUNDS_RESERVED = "ledgerflow.account.funds-reserved.v1";
    public static final String RESERVATION_REJECTED = "ledgerflow.account.funds-reservation-rejected.v1";
    public static final String RISK_APPROVED = "ledgerflow.risk.approved.v1";
    public static final String RISK_REJECTED = "ledgerflow.risk.rejected.v1";
    public static final String TRANSFER_SETTLED = "ledgerflow.account.transfer-settled.v1";
    public static final String FUNDS_RELEASED = "ledgerflow.account.funds-released.v1";
    public static final String SETTLEMENT_REQUESTED = "ledgerflow.transfer.settlement-requested.v1";
    public static final String COMPENSATION_REQUESTED = "ledgerflow.transfer.compensation-requested.v1";
    public static final String COMPLETED = "ledgerflow.transfer.completed.v1";
    public static final String REJECTED = "ledgerflow.transfer.rejected.v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferWorkflowService.class);

    private final TransferRepository transfers;
    private final TransferHistoryRepository history;
    private final OutboxRepository outbox;
    private final TransferProcessedEventStore processedEvents;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;
    private final MeterRegistry metrics;

    public TransferWorkflowService(
            TransferRepository transfers,
            TransferHistoryRepository history,
            OutboxRepository outbox,
            TransferProcessedEventStore processedEvents,
            Clock clock,
            Supplier<UUID> uuidGenerator,
            MeterRegistry metrics) {
        this.transfers = transfers;
        this.history = history;
        this.outbox = outbox;
        this.processedEvents = processedEvents;
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
        this.metrics = metrics;
    }

    @Transactional
    public void handle(WorkflowEnvelope envelope) {
        if (processedEvents.processed(envelope.eventId())) {
            metrics.counter("kafka.consumer.duplicate", "service", "transfer-service")
                    .increment();
            LOGGER.info(
                    "kafka_consumer_duplicate service=transfer-service eventId={} eventType={} correlationId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.correlationId());
            return;
        }
        UUID transferId = requiredTransferId(envelope);
        Transfer transfer = transfers
                .findByIdForUpdate(TransferId.from(transferId))
                .orElseThrow(() -> new TransferNotFoundException(transferId.toString()));
        switch (envelope.eventType()) {
            case FUNDS_RESERVED ->
                singleTransition(
                        transfer,
                        TransferStatus.PENDING,
                        TransferStatus.FUNDS_RESERVED,
                        "FUNDS_RESERVED",
                        envelope,
                        null);
            case RESERVATION_REJECTED ->
                singleTransition(
                        transfer,
                        TransferStatus.PENDING,
                        TransferStatus.REJECTED,
                        "FUNDS_RESERVATION_REJECTED",
                        envelope,
                        REJECTED);
            case RISK_APPROVED -> approveRisk(transfer, envelope);
            case RISK_REJECTED ->
                singleTransition(
                        transfer,
                        TransferStatus.FUNDS_RESERVED,
                        TransferStatus.COMPENSATING,
                        "RISK_REJECTED",
                        envelope,
                        COMPENSATION_REQUESTED);
            case TRANSFER_SETTLED ->
                singleTransition(
                        transfer,
                        TransferStatus.SETTLING,
                        TransferStatus.COMPLETED,
                        "TRANSFER_SETTLED",
                        envelope,
                        COMPLETED);
            case FUNDS_RELEASED ->
                singleTransition(
                        transfer,
                        TransferStatus.COMPENSATING,
                        TransferStatus.REJECTED,
                        "FUNDS_RELEASED",
                        envelope,
                        REJECTED);
            default -> throw new IllegalArgumentException("Unsupported Transfer Service event type");
        }
        processedEvents.markProcessed(envelope.eventId(), envelope.eventType(), clock.instant());
        metrics.counter("kafka.consumer.processed", "service", "transfer-service")
                .increment();
    }

    private void approveRisk(Transfer transfer, WorkflowEnvelope envelope) {
        if (transfer.status() != TransferStatus.FUNDS_RESERVED) {
            logStale(transfer, envelope);
            return;
        }
        Instant now = clock.instant();
        Transfer.Transition approved =
                transfer.transitionTo(TransferStatus.RISK_APPROVED, "RISK_APPROVED", now, uuidGenerator.get());
        transfers.save(approved.transfer());
        history.save(approved.history());
        Transfer.Transition settling = approved.transfer()
                .transitionTo(TransferStatus.SETTLING, "SETTLEMENT_REQUESTED", now, uuidGenerator.get());
        Transfer saved = transfers.save(settling.transfer());
        history.save(settling.history());
        append(
                saved,
                SETTLEMENT_REQUESTED,
                envelope,
                Map.of("transferId", saved.id().value()),
                now);
        logTransition(transfer, saved, envelope);
    }

    private void singleTransition(
            Transfer transfer,
            TransferStatus expected,
            TransferStatus target,
            String reason,
            WorkflowEnvelope envelope,
            String outboundType) {
        if (transfer.status() != expected) {
            logStale(transfer, envelope);
            return;
        }
        Instant now = clock.instant();
        Transfer.Transition transition = transfer.transitionTo(target, reason, now, uuidGenerator.get());
        Transfer saved = transfers.save(transition.transfer());
        history.save(transition.history());
        if (target == TransferStatus.COMPLETED) {
            metrics.counter("transfer.completed", "service", "transfer-service").increment();
        } else if (target == TransferStatus.REJECTED) {
            metrics.counter("transfer.rejected", "service", "transfer-service").increment();
        }
        if (outboundType != null) {
            Map<String, Object> payload = outboundType.equals(REJECTED)
                    ? Map.of("transferId", saved.id().value(), "reason", optionalReason(envelope))
                    : Map.of("transferId", saved.id().value());
            append(saved, outboundType, envelope, payload, now);
        }
        logTransition(transfer, saved, envelope);
    }

    private void append(
            Transfer transfer, String eventType, WorkflowEnvelope envelope, Map<String, Object> payload, Instant now) {
        outbox.save(new WorkflowOutboxEvent(
                uuidGenerator.get(),
                eventType,
                transfer,
                now,
                envelope.eventId().toString(),
                payload));
    }

    private static UUID requiredTransferId(WorkflowEnvelope envelope) {
        var value = envelope.payload().get("transferId");
        if (value == null || !value.isValueNode()) {
            throw new IllegalArgumentException("Workflow event transferId is required");
        }
        return UUID.fromString(value.asText());
    }

    private static String optionalReason(WorkflowEnvelope envelope) {
        var value = envelope.payload().get("reason");
        return value == null || value.asText().isBlank() ? "WORKFLOW_REJECTED" : value.asText();
    }

    private static void logTransition(Transfer before, Transfer after, WorkflowEnvelope envelope) {
        LOGGER.info(
                "transfer_state_changed service=transfer-service eventId={} eventType={} transferId={} correlationId={} currentState={} nextState={}",
                envelope.eventId(),
                envelope.eventType(),
                after.id(),
                after.correlationId(),
                before.status(),
                after.status());
    }

    private static void logStale(Transfer transfer, WorkflowEnvelope envelope) {
        LOGGER.info(
                "kafka_consumer_stale service=transfer-service eventId={} eventType={} transferId={} correlationId={} currentState={}",
                envelope.eventId(),
                envelope.eventType(),
                transfer.id(),
                transfer.correlationId(),
                transfer.status());
    }
}
