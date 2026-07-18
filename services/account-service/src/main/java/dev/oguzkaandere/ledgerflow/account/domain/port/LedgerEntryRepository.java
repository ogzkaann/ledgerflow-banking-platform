package dev.oguzkaandere.ledgerflow.account.domain.port;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerPage;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import java.util.List;

public interface LedgerEntryRepository {

    LedgerEntry save(LedgerEntry ledgerEntry);

    boolean existsByAccountIdAndReference(AccountId accountId, LedgerReference reference);

    LedgerPage findByAccountId(AccountId accountId, int page, int size);

    List<LedgerEntry> findAllByAccountId(AccountId accountId);
}
