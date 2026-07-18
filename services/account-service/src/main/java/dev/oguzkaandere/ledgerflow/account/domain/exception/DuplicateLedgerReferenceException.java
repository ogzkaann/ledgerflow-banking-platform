package dev.oguzkaandere.ledgerflow.account.domain.exception;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;

public final class DuplicateLedgerReferenceException extends RuntimeException {

    public DuplicateLedgerReferenceException(AccountId accountId, LedgerReference reference) {
        super("Reference " + reference + " has already been used for account " + accountId);
    }
}
