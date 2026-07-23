package dev.oguzkaandere.ledgerflow.risk.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecision;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecisionId;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskOutcome;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskReason;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskRuleVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_decisions")
class RiskDecisionJpaEntity {
    @Id
    @Column(name = "decision_id")
    UUID decisionId;

    @Column(name = "transfer_id", nullable = false, unique = true)
    UUID transferId;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false, length = 100)
    String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RiskOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    RiskReason reason;

    @Column(name = "rule_version", nullable = false, length = 50)
    String ruleVersion;

    @Column(name = "correlation_id", nullable = false, length = 100)
    String correlationId;

    @Column(name = "decided_at", nullable = false)
    Instant decidedAt;

    protected RiskDecisionJpaEntity() {}

    static RiskDecisionJpaEntity fromDomain(RiskDecision decision) {
        RiskDecisionJpaEntity entity = new RiskDecisionJpaEntity();
        entity.decisionId = decision.id().value();
        entity.transferId = decision.transferId();
        entity.amount = decision.amount();
        entity.currency = decision.currency();
        entity.reference = decision.reference();
        entity.outcome = decision.outcome();
        entity.reason = decision.reason();
        entity.ruleVersion = decision.ruleVersion().value();
        entity.correlationId = decision.correlationId();
        entity.decidedAt = decision.decidedAt();
        return entity;
    }

    RiskDecision toDomain() {
        return new RiskDecision(
                new RiskDecisionId(decisionId),
                transferId,
                amount,
                currency,
                reference,
                outcome,
                reason,
                new RiskRuleVersion(ruleVersion),
                correlationId,
                decidedAt);
    }
}
