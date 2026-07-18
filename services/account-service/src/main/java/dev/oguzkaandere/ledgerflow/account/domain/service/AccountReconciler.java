package dev.oguzkaandere.ledgerflow.account.domain.service;

import dev.oguzkaandere.ledgerflow.account.domain.exception.ReconciliationException;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.ReconciliationResult;
import java.math.BigDecimal;
import java.util.List;

public final class AccountReconciler {

    public ReconciliationResult reconcile(Account account, List<LedgerEntry> entries) {
        BigDecimal ledgerBalance = entries.stream()
                .map(entry -> {
                    if (entry.amount().currency() != account.currency()) {
                        throw new IllegalArgumentException("Ledger entry currency must match the account currency");
                    }
                    return entry.type().applySign(entry.amount().amount());
                })
                .reduce(BigDecimal.ZERO.setScale(Money.SCALE), BigDecimal::add);
        BigDecimal materializedBalance = account.availableBalance()
                .amount()
                .add(account.reservedBalance().amount());
        boolean balanced = ledgerBalance.compareTo(materializedBalance) == 0;
        return new ReconciliationResult(account.id(), ledgerBalance, materializedBalance, balanced);
    }

    public void verify(Account account, List<LedgerEntry> entries) {
        ReconciliationResult result = reconcile(account, entries);
        if (!result.balanced()) {
            throw new ReconciliationException(account.id(), result.ledgerBalance(), result.materializedBalance());
        }
    }
}
