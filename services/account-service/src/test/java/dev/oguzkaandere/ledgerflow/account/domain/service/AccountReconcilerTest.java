package dev.oguzkaandere.ledgerflow.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.account.domain.exception.ReconciliationException;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountStatus;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntryType;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.ReconciliationResult;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountReconcilerTest {

    private final AccountReconciler reconciler = new AccountReconciler();

    @Test
    void balancesCreditsAndDebitsAgainstMaterializedBalances() {
        AccountId accountId = AccountId.from(UUID.randomUUID());
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        Account account = new Account(
                accountId,
                "customer-001",
                SupportedCurrency.GBP,
                AccountStatus.ACTIVE,
                new Money(new BigDecimal("90.00"), SupportedCurrency.GBP),
                new Money(new BigDecimal("10.00"), SupportedCurrency.GBP),
                2,
                now,
                now);
        List<LedgerEntry> entries = List.of(
                entry(accountId, LedgerEntryType.CREDIT, "125.00", "credit", now),
                entry(accountId, LedgerEntryType.DEBIT, "25.00", "debit", now.plusSeconds(1)));

        ReconciliationResult result = reconciler.reconcile(account, entries);

        assertThat(result.balanced()).isTrue();
        assertThat(result.ledgerBalance()).isEqualByComparingTo("100.00");
        assertThat(result.materializedBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void verificationRejectsAnUnbalancedAccount() {
        Account account = Account.create(
                AccountId.from(UUID.randomUUID()),
                "customer-001",
                SupportedCurrency.GBP,
                Instant.parse("2026-07-18T12:00:00Z"));

        assertThatThrownBy(() -> reconciler.verify(
                        account,
                        List.of(entry(
                                account.id(),
                                LedgerEntryType.CREDIT,
                                "1.00",
                                "unexpected-credit",
                                account.createdAt()))))
                .isInstanceOf(ReconciliationException.class);
    }

    private static LedgerEntry entry(
            AccountId accountId, LedgerEntryType type, String amount, String reference, Instant createdAt) {
        return new LedgerEntry(
                UUID.randomUUID(),
                accountId,
                type,
                new Money(new BigDecimal(amount), SupportedCurrency.GBP),
                new LedgerReference(reference),
                createdAt);
    }
}
