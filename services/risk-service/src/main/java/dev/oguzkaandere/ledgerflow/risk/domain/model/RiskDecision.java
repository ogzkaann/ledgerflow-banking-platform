package dev.oguzkaandere.ledgerflow.risk.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RiskDecision(
        RiskDecisionId id,
        UUID transferId,
        BigDecimal amount,
        String currency,
        String reference,
        RiskOutcome outcome,
        RiskReason reason,
        RiskRuleVersion ruleVersion,
        String correlationId,
        Instant decidedAt) {
    public RiskDecision {
        if (id == null
                || transferId == null
                || amount == null
                || amount.signum() <= 0
                || amount.scale() > 2
                || currency == null
                || !java.util.Set.of("EUR", "USD", "GBP").contains(currency)
                || reference == null
                || reference.isBlank()
                || outcome == null
                || reason == null
                || ruleVersion == null
                || correlationId == null
                || correlationId.isBlank()
                || decidedAt == null) {
            throw new IllegalArgumentException("Invalid risk decision");
        }
        amount = amount.setScale(2);
        reference = reference.trim();
    }
}
