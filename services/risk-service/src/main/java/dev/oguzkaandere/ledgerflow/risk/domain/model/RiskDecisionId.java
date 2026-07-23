package dev.oguzkaandere.ledgerflow.risk.domain.model;

import java.util.UUID;

public record RiskDecisionId(UUID value) {
    public RiskDecisionId {
        if (value == null) {
            throw new IllegalArgumentException("Risk decision ID is required");
        }
    }
}
