package dev.oguzkaandere.ledgerflow.account.domain.port;

import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountPage;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountSearchCriteria;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId accountId);

    Optional<Account> findByIdForUpdate(AccountId accountId);

    AccountPage findPage(AccountSearchCriteria criteria);
}
