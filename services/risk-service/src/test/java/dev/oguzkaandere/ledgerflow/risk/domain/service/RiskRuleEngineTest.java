package dev.oguzkaandere.ledgerflow.risk.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskOutcome;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskReason;
import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskRuleVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RiskRuleEngineTest {
    private final RiskRuleEngine rules =
            new RiskRuleEngine(new BigDecimal("5000.00"), "RISK-REJECT", new RiskRuleVersion("risk-rules-v1"));

    @Test
    void approvesBelowThreshold() {
        assertThat(decide("125.50", "normal").outcome()).isEqualTo(RiskOutcome.APPROVED);
        assertThat(decide("125.50", "normal").reason()).isEqualTo(RiskReason.RULES_PASSED);
    }

    @Test
    void rejectsAboveThresholdAndBlockedMarkerDeterministically() {
        assertThat(decide("5000.01", "normal").reason()).isEqualTo(RiskReason.AMOUNT_LIMIT_EXCEEDED);
        assertThat(decide("125.50", "invoice-RISK-REJECT-1").reason()).isEqualTo(RiskReason.BLOCKED_REFERENCE);
        assertThat(decide("125.50", "invoice-RISK-REJECT-1").outcome()).isEqualTo(RiskOutcome.REJECTED);
    }

    private dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecision decide(String amount, String reference) {
        return rules.evaluate(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                new BigDecimal(amount),
                "EUR",
                reference,
                "correlation",
                Instant.parse("2026-07-23T12:00:00Z"));
    }
}
