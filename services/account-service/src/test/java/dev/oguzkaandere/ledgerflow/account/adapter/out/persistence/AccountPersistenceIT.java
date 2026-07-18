package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.account.application.command.CreateAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.command.FundAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.service.AccountApplicationService;
import dev.oguzkaandere.ledgerflow.account.domain.exception.DuplicateLedgerReferenceException;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerPage;
import dev.oguzkaandere.ledgerflow.account.domain.model.ReconciliationResult;
import dev.oguzkaandere.ledgerflow.account.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

class AccountPersistenceIT extends PostgresIntegrationTest {

    @Autowired
    private AccountApplicationService accountService;

    @Test
    void databaseCheckConstraintsRejectInvalidBalances() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO accounts (
                            id, owner_reference, currency, status, available_balance,
                            reserved_balance, version, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        "customer-invalid",
                        "EUR",
                        "ACTIVE",
                        new BigDecimal("-0.01"),
                        BigDecimal.ZERO,
                        0,
                        Timestamp.from(Instant.now()),
                        Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void accountAndLedgerArePersistedAtomicallyAndCanBeReconciled() {
        Account created = createAccount("customer-persisted");

        accountService.addTestFunding(
                new FundAccountCommand(created.id(), new BigDecimal("125.25"), "persisted-funding"));

        Account reloaded = accountService.getAccount(created.id());
        LedgerPage ledger = accountService.getLedger(created.id(), 0, 20);
        ReconciliationResult reconciliation = accountService.reconcile(created.id());
        assertThat(reloaded.availableBalance().amount()).isEqualByComparingTo("125.25");
        assertThat(reloaded.version()).isEqualTo(1);
        assertThat(ledger.totalElements()).isEqualTo(1);
        assertThat(ledger.content().getFirst().reference().value()).isEqualTo("persisted-funding");
        assertThat(reconciliation.balanced()).isTrue();
    }

    @Test
    void uniqueConstraintProtectsFundingReferencePerAccount() {
        Account account = createAccount("customer-unique");
        accountService.addTestFunding(
                new FundAccountCommand(account.id(), new BigDecimal("10.00"), "unique-reference"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO ledger_entries (
                            id, account_id, entry_type, amount, currency, reference, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        account.id().value(),
                        "CREDIT",
                        new BigDecimal("10.00"),
                        "EUR",
                        "unique-reference",
                        Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> accountService.addTestFunding(
                        new FundAccountCommand(account.id(), new BigDecimal("10.00"), "unique-reference")))
                .isInstanceOf(DuplicateLedgerReferenceException.class);
    }

    @Test
    void transactionRollsBackLedgerInsertWhenAccountUpdateFails() {
        Account account = createAccount("customer-rollback");
        jdbcTemplate.execute("""
                CREATE FUNCTION ledgerflow_test_fail_account_update() RETURNS trigger
                LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced account update failure'; END $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER ledgerflow_test_account_update
                BEFORE UPDATE ON accounts FOR EACH ROW
                EXECUTE FUNCTION ledgerflow_test_fail_account_update()
                """);
        try {
            assertThatThrownBy(() -> accountService.addTestFunding(
                            new FundAccountCommand(account.id(), new BigDecimal("20.00"), "rollback-reference")))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("DROP TRIGGER ledgerflow_test_account_update ON accounts");
            jdbcTemplate.execute("DROP FUNCTION ledgerflow_test_fail_account_update()");
        }

        assertThat(accountService.getAccount(account.id()).availableBalance().amount())
                .isEqualByComparingTo("0.00");
        assertThat(accountService.getLedger(account.id(), 0, 20).totalElements())
                .isZero();
    }

    @Test
    void concurrentFundingUsesRowLockAndDoesNotLoseCredits() throws Exception {
        Account account = createAccount("customer-concurrent");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> fundWhenReleased(account, "concurrent-1", ready, start));
            var second = executor.submit(() -> fundWhenReleased(account, "concurrent-2", ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }

        Account reloaded = accountService.getAccount(account.id());
        assertThat(reloaded.availableBalance().amount()).isEqualByComparingTo("200.00");
        assertThat(reloaded.version()).isEqualTo(2);
        assertThat(accountService.getLedger(account.id(), 0, 20).totalElements())
                .isEqualTo(2);
    }

    @Test
    void ledgerPaginationIsBoundedAndNewestFirst() {
        Account account = createAccount("customer-pagination");
        accountService.addTestFunding(new FundAccountCommand(account.id(), new BigDecimal("1.00"), "first"));
        accountService.addTestFunding(new FundAccountCommand(account.id(), new BigDecimal("2.00"), "second"));
        accountService.addTestFunding(new FundAccountCommand(account.id(), new BigDecimal("3.00"), "third"));
        setCreatedAt(account, "first", Instant.parse("2026-07-18T12:00:01Z"));
        setCreatedAt(account, "second", Instant.parse("2026-07-18T12:00:02Z"));
        setCreatedAt(account, "third", Instant.parse("2026-07-18T12:00:03Z"));

        LedgerPage firstPage = accountService.getLedger(account.id(), 0, 2);
        LedgerPage secondPage = accountService.getLedger(account.id(), 1, 2);

        assertThat(firstPage.content())
                .extracting(entry -> entry.reference().value())
                .containsExactly("third", "second");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content())
                .extracting(entry -> entry.reference().value())
                .containsExactly("first");
    }

    private Account createAccount(String ownerReference) {
        return accountService.createAccount(new CreateAccountCommand(ownerReference, "EUR"));
    }

    private void fundWhenReleased(Account account, String reference, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent funding start timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent funding was interrupted", exception);
        }
        accountService.addTestFunding(new FundAccountCommand(account.id(), new BigDecimal("100.00"), reference));
    }

    private void setCreatedAt(Account account, String reference, Instant createdAt) {
        jdbcTemplate.update(
                "UPDATE ledger_entries SET created_at = ? WHERE account_id = ? AND reference = ?",
                Timestamp.from(createdAt),
                account.id().value(),
                reference);
    }
}
