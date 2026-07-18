package dev.oguzkaandere.ledgerflow.account.domain.exception;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountStatus;

public final class AccountStateException extends RuntimeException {

    public AccountStateException(AccountId accountId, AccountStatus status) {
        super("Account " + accountId + " cannot be mutated while its status is " + status);
    }
}
