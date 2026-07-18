package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLedgerEntryRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    boolean existsByAccountIdAndReference(UUID accountId, String reference);

    Page<LedgerEntryJpaEntity> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);

    List<LedgerEntryJpaEntity> findAllByAccountIdOrderByCreatedAtAscIdAsc(UUID accountId);
}
