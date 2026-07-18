package dev.oguzkaandere.ledgerflow.account.domain.exception;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;

public final class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(AccountId accountId) {
        super("Account " + accountId + " does not exist");
    }
}
