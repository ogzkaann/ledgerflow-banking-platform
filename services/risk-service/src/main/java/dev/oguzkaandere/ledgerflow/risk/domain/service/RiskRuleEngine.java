package dev.oguzkaandere.ledgerflow.risk.domain.service;

import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecision;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecisionId;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskOutcome;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskReason;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskRuleVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class RiskRuleEngine {
    private final BigDecimal maximumAmount;
    private final String blockedMarker;
    private final RiskRuleVersion ruleVersion;

    public RiskRuleEngine(BigDecimal maximumAmount, String blockedMarker, RiskRuleVersion ruleVersion) {
        if (maximumAmount == null || maximumAmount.signum() <= 0 || blockedMarker == null || blockedMarker.isBlank()) {
            throw new IllegalArgumentException("Risk rule configuration is invalid");
        }
        this.maximumAmount = maximumAmount;
        this.blockedMarker = blockedMarker;
        this.ruleVersion = ruleVersion;
    }

    public RiskDecision evaluate(
            UUID decisionId,
            UUID transferId,
            BigDecimal amount,
            String currency,
            String reference,
            String correlationId,
            Instant now) {
        RiskOutcome outcome = RiskOutcome.APPROVED;
        RiskReason reason = RiskReason.RULES_PASSED;
        if (amount.compareTo(maximumAmount) > 0) {
            outcome = RiskOutcome.REJECTED;
            reason = RiskReason.AMOUNT_LIMIT_EXCEEDED;
        } else if (reference.contains(blockedMarker)) {
            outcome = RiskOutcome.REJECTED;
            reason = RiskReason.BLOCKED_REFERENCE;
        }
        return new RiskDecision(
                new RiskDecisionId(decisionId),
                transferId,
                amount,
                currency,
                reference,
                outcome,
                reason,
                ruleVersion,
                correlationId,
                now);
    }
}
