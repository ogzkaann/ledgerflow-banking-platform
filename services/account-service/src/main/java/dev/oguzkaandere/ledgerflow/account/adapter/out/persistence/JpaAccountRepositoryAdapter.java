package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.port.AccountRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaAccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountRepository repository;

    JpaAccountRepositoryAdapter(SpringDataAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = repository.findById(account.id().value()).orElse(null);
        if (entity == null) {
            entity = AccountJpaEntity.fromDomain(account);
        } else {
            entity.updateFrom(account);
        }
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return repository.findById(accountId.value()).map(AccountJpaEntity::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(AccountId accountId) {
        return repository.findByIdForUpdate(accountId.value()).map(AccountJpaEntity::toDomain);
    }
}
