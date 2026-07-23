package dev.oguzkaandere.ledgerflow.risk.application.service;

import dev.oguzkaandere.ledgerflow.risk.adapter.out.eventing.RiskEventStore;
import dev.oguzkaandere.ledgerflow.risk.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecision;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskOutcome;
import dev.oguzkaandere.ledgerflow.risk.domain.port.RiskDecisionRepository;
import dev.oguzkaandere.ledgerflow.risk.domain.service.RiskRuleEngine;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
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
public class RiskWorkflowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RiskWorkflowService.class);
    private static final String FUNDS_RESERVED = "ledgerflow.account.funds-reserved.v1";
    private static final String APPROVED = "ledgerflow.risk.approved.v1";
    private static final String REJECTED = "ledgerflow.risk.rejected.v1";

    private final RiskDecisionRepository decisions;
    private final RiskRuleEngine rules;
    private final RiskEventStore events;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;
    private final MeterRegistry metrics;

    public RiskWorkflowService(
            RiskDecisionRepository decisions,
            RiskRuleEngine rules,
            RiskEventStore events,
            Clock clock,
            Supplier<UUID> uuidGenerator,
            MeterRegistry metrics) {
        this.decisions = decisions;
        this.rules = rules;
        this.events = events;
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
        this.metrics = metrics;
    }

    @Transactional
    public void handle(WorkflowEnvelope envelope) {
        if (!FUNDS_RESERVED.equals(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported Risk Service event type");
        }
        if (events.processed(envelope.eventId())) {
            metrics.counter("kafka.consumer.duplicate", "service", "risk-service")
                    .increment();
            LOGGER.info(
                    "kafka_consumer_duplicate service=risk-service eventId={} eventType={} correlationId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.correlationId());
            return;
        }
        UUID transferId = UUID.fromString(required(envelope, "transferId"));
        if (decisions.findByTransferId(transferId).isPresent()) {
            events.markProcessed(envelope.eventId(), envelope.eventType(), clock.instant());
            metrics.counter("kafka.consumer.processed", "service", "risk-service")
                    .increment();
            return;
        }
        Instant now = clock.instant();
        RiskDecision decision = rules.evaluate(
                uuidGenerator.get(),
                transferId,
                new BigDecimal(required(envelope, "amount")),
                required(envelope, "currency"),
                required(envelope, "reference"),
                envelope.correlationId(),
                now);
        decisions.save(decision);
        String eventType = decision.outcome() == RiskOutcome.APPROVED ? APPROVED : REJECTED;
        events.append(
                eventType,
                transferId,
                envelope.correlationId(),
                envelope.eventId().toString(),
                Map.of(
                        "transferId", transferId,
                        "outcome", decision.outcome().name(),
                        "reason", decision.reason().name(),
                        "ruleVersion", decision.ruleVersion().value()),
                now);
        events.markProcessed(envelope.eventId(), envelope.eventType(), now);
        metrics.counter("kafka.consumer.processed", "service", "risk-service").increment();
        metrics.counter(
                        decision.outcome() == RiskOutcome.APPROVED ? "risk.approved" : "risk.rejected",
                        "service",
                        "risk-service")
                .increment();
        LOGGER.info(
                "risk_decided service=risk-service eventId={} transferId={} correlationId={} outcome={} reason={} ruleVersion={}",
                envelope.eventId(),
                transferId,
                envelope.correlationId(),
                decision.outcome(),
                decision.reason(),
                decision.ruleVersion().value());
    }

    private static String required(WorkflowEnvelope envelope, String field) {
        var value = envelope.payload().get(field);
        if (value == null || !value.isValueNode() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Required risk payload field is missing: " + field);
        }
        return value.asText();
    }
}
