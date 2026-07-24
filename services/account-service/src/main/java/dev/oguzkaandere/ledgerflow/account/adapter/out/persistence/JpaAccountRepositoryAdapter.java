package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountPage;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountSearchCriteria;
import dev.oguzkaandere.ledgerflow.account.domain.port.AccountRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Override
    public AccountPage findPage(AccountSearchCriteria criteria) {
        Specification<AccountJpaEntity> specification = (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.currency() != null) {
                predicates.add(builder.equal(root.get("currency"), criteria.currency()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        var result = repository.findAll(specification, PageRequest.of(criteria.page(), criteria.size(), sort));
        return new AccountPage(
                result.getContent().stream().map(AccountJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
