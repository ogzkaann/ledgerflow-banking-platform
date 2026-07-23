package dev.oguzkaandere.ledgerflow.risk.domain.port;

import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecision;
import java.util.Optional;
import java.util.UUID;

public interface RiskDecisionRepository {
    RiskDecision save(RiskDecision decision);

    Optional<RiskDecision> findByTransferId(UUID transferId);
}
