package dev.oguzkaandere.ledgerflow.risk.application.config;

import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskRuleVersion;
import dev.oguzkaandere.ledgerflow.risk.domain.service.RiskRuleEngine;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RiskConfiguration {
    @Bean
    Clock riskClock() {
        return Clock.systemUTC();
    }

    @Bean
    Supplier<UUID> riskUuidGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    RiskRuleEngine riskRuleEngine(
            @Value("${ledgerflow.risk.maximum-amount:5000.00}") BigDecimal maximumAmount,
            @Value("${ledgerflow.risk.blocked-marker:RISK-REJECT}") String blockedMarker,
            @Value("${ledgerflow.risk.rule-version:risk-rules-v1}") String ruleVersion) {
        return new RiskRuleEngine(maximumAmount, blockedMarker, new RiskRuleVersion(ruleVersion));
    }
}
