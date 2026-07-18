package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.exception.DuplicateLedgerReferenceException;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerPage;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import dev.oguzkaandere.ledgerflow.account.domain.port.LedgerEntryRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class JpaLedgerEntryRepositoryAdapter implements LedgerEntryRepository {

    private static final String UNIQUE_REFERENCE_CONSTRAINT = "uk_ledger_entries_account_reference";

    private final SpringDataLedgerEntryRepository repository;

    JpaLedgerEntryRepositoryAdapter(SpringDataLedgerEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public LedgerEntry save(LedgerEntry ledgerEntry) {
        try {
            return repository
                    .saveAndFlush(LedgerEntryJpaEntity.fromDomain(ledgerEntry))
                    .toDomain();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, UNIQUE_REFERENCE_CONSTRAINT)) {
                throw new DuplicateLedgerReferenceException(ledgerEntry.accountId(), ledgerEntry.reference());
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByAccountIdAndReference(AccountId accountId, LedgerReference reference) {
        return repository.existsByAccountIdAndReference(accountId.value(), reference.value());
    }

    @Override
    public LedgerPage findByAccountId(AccountId accountId, int page, int size) {
        Page<LedgerEntryJpaEntity> result =
                repository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId.value(), PageRequest.of(page, size));
        return new LedgerPage(
                result.getContent().stream().map(LedgerEntryJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    public List<LedgerEntry> findAllByAccountId(AccountId accountId) {
        return repository.findAllByAccountIdOrderByCreatedAtAscIdAsc(accountId.value()).stream()
                .map(LedgerEntryJpaEntity::toDomain)
                .toList();
    }

    private boolean containsConstraint(Throwable throwable, String constraint) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(constraint)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
