package dev.oguzkaandere.ledgerflow.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.oguzkaandere.ledgerflow.account.application.command.CreateAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.command.FundAccountCommand;
import dev.oguzkaandere.ledgerflow.account.domain.exception.AccountNotFoundException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.DuplicateLedgerReferenceException;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.account.domain.port.AccountRepository;
import dev.oguzkaandere.ledgerflow.account.domain.port.LedgerEntryRepository;
import dev.oguzkaandere.ledgerflow.account.domain.service.AccountReconciler;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-18T12:00:00Z");
    private static final UUID ACCOUNT_UUID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID LEDGER_UUID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private AccountApplicationService service;

    @BeforeEach
    void setUp() {
        Supplier<UUID> ids = () -> LEDGER_UUID;
        service = new AccountApplicationService(
                accountRepository,
                ledgerEntryRepository,
                new AccountReconciler(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                ids);
    }

    @Test
    void createsAnActiveZeroBalanceAccount() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = service.createAccount(new CreateAccountCommand("customer-001", "eur"));

        assertThat(account.id().value()).isEqualTo(LEDGER_UUID);
        assertThat(account.currency()).isEqualTo(SupportedCurrency.EUR);
        assertThat(account.availableBalance().formattedAmount()).isEqualTo("0.00");
        assertThat(account.reservedBalance().formattedAmount()).isEqualTo("0.00");
    }

    @Test
    void retrievesAnExistingAccount() {
        Account account = existingAccount();
        when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));

        assertThat(service.getAccount(account.id())).isSameAs(account);
    }

    @Test
    void missingAccountReturnsTypedFailure() {
        AccountId accountId = AccountId.from(ACCOUNT_UUID);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(accountId)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void fundingWritesLedgerAndUpdatedAccount() {
        Account account = existingAccount();
        when(accountRepository.findByIdForUpdate(account.id())).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.existsByAccountIdAndReference(account.id(), new LedgerReference("fund-001")))
                .thenReturn(false);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FundingResult result =
                service.addTestFunding(new FundAccountCommand(account.id(), new BigDecimal("50.00"), "fund-001"));

        assertThat(result.account().availableBalance().amount()).isEqualByComparingTo("50.00");
        assertThat(result.ledgerEntry().ledgerEntryId()).isEqualTo(LEDGER_UUID);
        assertThat(result.ledgerEntry().amount().currency()).isEqualTo(SupportedCurrency.EUR);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void duplicateFundingReferenceIsRejectedBeforeMutation() {
        Account account = existingAccount();
        LedgerReference reference = new LedgerReference("fund-001");
        when(accountRepository.findByIdForUpdate(account.id())).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.existsByAccountIdAndReference(account.id(), reference))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addTestFunding(
                        new FundAccountCommand(account.id(), new BigDecimal("50.00"), reference.value())))
                .isInstanceOf(DuplicateLedgerReferenceException.class);

        verify(ledgerEntryRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void persistenceFailureStopsAccountUpdateSoTransactionCanRollBack() {
        Account account = existingAccount();
        when(accountRepository.findByIdForUpdate(account.id())).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.existsByAccountIdAndReference(any(), any())).thenReturn(false);
        when(ledgerEntryRepository.save(any()))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(() -> service.addTestFunding(
                        new FundAccountCommand(account.id(), new BigDecimal("50.00"), "fund-001")))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(accountRepository, never()).save(any());
    }

    private static Account existingAccount() {
        return Account.create(AccountId.from(ACCOUNT_UUID), "customer-001", SupportedCurrency.EUR, NOW);
    }
}
