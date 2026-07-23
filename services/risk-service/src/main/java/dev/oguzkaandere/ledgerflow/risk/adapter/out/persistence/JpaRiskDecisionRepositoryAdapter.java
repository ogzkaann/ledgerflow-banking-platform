package dev.oguzkaandere.ledgerflow.risk.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.risk.domain.model.RiskDecision;
import dev.oguzkaandere.ledgerflow.risk.domain.port.RiskDecisionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
class JpaRiskDecisionRepositoryAdapter implements RiskDecisionRepository {
    private final SpringDataRiskDecisionRepository repository;

    JpaRiskDecisionRepositoryAdapter(SpringDataRiskDecisionRepository repository) {
        this.repository = repository;
    }

    @Override
    public RiskDecision save(RiskDecision decision) {
        return repository
                .saveAndFlush(RiskDecisionJpaEntity.fromDomain(decision))
                .toDomain();
    }

    @Override
    public Optional<RiskDecision> findByTransferId(UUID transferId) {
        return repository.findByTransferId(transferId).map(RiskDecisionJpaEntity::toDomain);
    }
}

interface SpringDataRiskDecisionRepository extends JpaRepository<RiskDecisionJpaEntity, UUID> {
    Optional<RiskDecisionJpaEntity> findByTransferId(UUID transferId);
}
