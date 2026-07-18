package dev.oguzkaandere.ledgerflow.account.application.service;

import dev.oguzkaandere.ledgerflow.account.application.command.CreateAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.command.FundAccountCommand;
import dev.oguzkaandere.ledgerflow.account.domain.exception.AccountNotFoundException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.DuplicateLedgerReferenceException;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerPage;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.ReconciliationResult;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.account.domain.port.AccountRepository;
import dev.oguzkaandere.ledgerflow.account.domain.port.LedgerEntryRepository;
import dev.oguzkaandere.ledgerflow.account.domain.service.AccountReconciler;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountApplicationService.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountReconciler reconciler;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;

    public AccountApplicationService(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AccountReconciler reconciler,
            Clock clock,
            Supplier<UUID> uuidGenerator) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.reconciler = reconciler;
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
    }

    @Transactional
    public Account createAccount(CreateAccountCommand command) {
        SupportedCurrency currency = SupportedCurrency.fromCode(command.currency());
        Instant now = clock.instant();
        Account account = Account.create(AccountId.from(uuidGenerator.get()), command.ownerReference(), currency, now);
        Account saved = accountRepository.save(account);
        LOGGER.info("account_created accountId={} currency={}", saved.id(), saved.currency());
        return saved;
    }

    @Transactional(readOnly = true)
    public Account getAccount(AccountId accountId) {
        return accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    public LedgerPage getLedger(AccountId accountId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        return ledgerEntryRepository.findByAccountId(accountId, page, size);
    }

    @Transactional
    public FundingResult addTestFunding(FundAccountCommand command) {
        Account account = accountRepository
                .findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));
        LedgerReference reference = new LedgerReference(command.reference());
        if (ledgerEntryRepository.existsByAccountIdAndReference(account.id(), reference)) {
            LOGGER.warn("synthetic_funding_duplicate accountId={} reference={}", account.id(), reference);
            throw new DuplicateLedgerReferenceException(account.id(), reference);
        }

        Money amount = Money.positive(command.amount(), account.currency());
        Instant now = clock.instant();
        LedgerEntry ledgerEntry = LedgerEntry.credit(uuidGenerator.get(), account.id(), amount, reference, now);
        Account creditedAccount = account.credit(amount, now);

        try {
            LedgerEntry savedEntry = ledgerEntryRepository.save(ledgerEntry);
            Account savedAccount = accountRepository.save(creditedAccount);
            LOGGER.info(
                    "synthetic_funding_accepted accountId={} ledgerEntryId={} reference={} amount={} currency={}",
                    savedAccount.id(),
                    savedEntry.ledgerEntryId(),
                    savedEntry.reference(),
                    savedEntry.amount().formattedAmount(),
                    savedEntry.amount().currency());
            return new FundingResult(savedAccount, savedEntry);
        } catch (DuplicateLedgerReferenceException exception) {
            LOGGER.warn("synthetic_funding_duplicate accountId={} reference={}", account.id(), reference);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ReconciliationResult reconcile(AccountId accountId) {
        Account account = getAccount(accountId);
        ReconciliationResult result =
                reconciler.reconcile(account, ledgerEntryRepository.findAllByAccountId(accountId));
        if (!result.balanced()) {
            LOGGER.error(
                    "account_reconciliation_failed accountId={} ledgerBalance={} materializedBalance={}",
                    accountId,
                    result.ledgerBalance().toPlainString(),
                    result.materializedBalance().toPlainString());
        }
        return result;
    }
}
