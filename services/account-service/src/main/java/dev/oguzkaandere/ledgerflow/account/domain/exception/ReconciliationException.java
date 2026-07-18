package dev.oguzkaandere.ledgerflow.account.domain.exception;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import java.math.BigDecimal;

public final class ReconciliationException extends RuntimeException {

    public ReconciliationException(AccountId accountId, BigDecimal ledgerBalance, BigDecimal materializedBalance) {
        super("Account " + accountId + " failed reconciliation: ledger balance " + ledgerBalance.toPlainString()
                + " differs from materialized balance " + materializedBalance.toPlainString());
    }
}
