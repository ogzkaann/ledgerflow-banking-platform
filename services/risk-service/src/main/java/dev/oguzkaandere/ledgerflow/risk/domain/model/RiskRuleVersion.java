package dev.oguzkaandere.ledgerflow.risk.domain.model;

public record RiskRuleVersion(String value) {
    public RiskRuleVersion {
        if (value == null || value.isBlank() || value.length() > 50) {
            throw new IllegalArgumentException("Risk rule version is required and bounded");
        }
        value = value.trim();
    }
}
