package dev.oguzkaandere.ledgerflow.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.account.application.command.CreateAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.command.FundAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

class AccountTransferWorkflowServiceIT extends PostgresIntegrationTest {
    @Autowired
    private AccountApplicationService accounts;

    @Autowired
    private AccountTransferWorkflowService workflow;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void reservesAndSettlesExactlyOnceWithReconciledLedger() {
        Account source = accounts.createAccount(new CreateAccountCommand("source", "EUR"));
        Account destination = accounts.createAccount(new CreateAccountCommand("destination", "EUR"));
        accounts.addTestFunding(new FundAccountCommand(source.id(), new BigDecimal("1000.00"), "initial-funding"));
        UUID transferId = UUID.randomUUID();
        WorkflowEnvelope initiated = initiated(transferId, source, destination, "125.50", "normal-reference");

        workflow.handle(initiated);
        Account reservedSource = accounts.getAccount(source.id());
        assertThat(reservedSource.availableBalance().formattedAmount()).isEqualTo("874.50");
        assertThat(reservedSource.reservedBalance().formattedAmount()).isEqualTo("125.50");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM transfer_reservations", Integer.class))
                .isEqualTo(1);

        WorkflowEnvelope settlement =
                command(AccountTransferWorkflowService.SETTLEMENT_REQUESTED, transferId, "correlation-1");
        workflow.handle(settlement);
        workflow.handle(settlement);

        Account settledSource = accounts.getAccount(source.id());
        Account settledDestination = accounts.getAccount(destination.id());
        assertThat(settledSource.availableBalance().formattedAmount()).isEqualTo("874.50");
        assertThat(settledSource.reservedBalance().formattedAmount()).isEqualTo("0.00");
        assertThat(settledDestination.availableBalance().formattedAmount()).isEqualTo("125.50");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM ledger_entries WHERE reference LIKE ?",
                        Integer.class,
                        "transfer:" + transferId + ":%"))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM transfer_reservations WHERE transfer_id=?", String.class, transferId))
                .isEqualTo("SETTLED");
        assertThat(accounts.reconcile(source.id()).balanced()).isTrue();
        assertThat(accounts.reconcile(destination.id()).balanced()).isTrue();
    }

    @Test
    void releasesReservationWithoutTransferLedgerEntries() {
        Account source = accounts.createAccount(new CreateAccountCommand("source-release", "EUR"));
        Account destination = accounts.createAccount(new CreateAccountCommand("destination-release", "EUR"));
        accounts.addTestFunding(new FundAccountCommand(source.id(), new BigDecimal("1000.00"), "release-funding"));
        UUID transferId = UUID.randomUUID();
        workflow.handle(initiated(transferId, source, destination, "125.50", "RISK-REJECT"));

        WorkflowEnvelope compensation =
                command(AccountTransferWorkflowService.COMPENSATION_REQUESTED, transferId, "correlation-1");
        workflow.handle(compensation);
        workflow.handle(compensation);

        Account released = accounts.getAccount(source.id());
        assertThat(released.availableBalance().formattedAmount()).isEqualTo("1000.00");
        assertThat(released.reservedBalance().formattedAmount()).isEqualTo("0.00");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM ledger_entries WHERE reference LIKE ?",
                        Integer.class,
                        "transfer:" + transferId + ":%"))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM transfer_reservations WHERE transfer_id=?", String.class, transferId))
                .isEqualTo("RELEASED");
    }

    @Test
    void insufficientFundsProducesBusinessEventWithoutReservation() {
        Account source = accounts.createAccount(new CreateAccountCommand("empty-source", "EUR"));
        Account destination = accounts.createAccount(new CreateAccountCommand("destination", "EUR"));

        workflow.handle(initiated(UUID.randomUUID(), source, destination, "125.50", "normal"));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM transfer_reservations", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT payload #>> '{payload,reason}' FROM outbox_events", String.class))
                .isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM processed_events", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void missingSourceAndDestinationProduceStableBusinessReasons() {
        Account destination = accounts.createAccount(new CreateAccountCommand("only-destination", "EUR"));
        workflow.handle(initiated(
                UUID.randomUUID(), UUID.randomUUID(), destination.id().value(), "125.50", "EUR", "missing-source"));
        assertThat(lastRejectionReason()).isEqualTo("SOURCE_ACCOUNT_NOT_FOUND");

        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM processed_events");
        jdbcTemplate.update("DELETE FROM accounts");
        Account source = accounts.createAccount(new CreateAccountCommand("only-source", "EUR"));
        workflow.handle(initiated(
                UUID.randomUUID(), source.id().value(), UUID.randomUUID(), "125.50", "EUR", "missing-destination"));
        assertThat(lastRejectionReason()).isEqualTo("DESTINATION_ACCOUNT_NOT_FOUND");
    }

    @Test
    void inactiveSourceAndCurrencyMismatchProduceStableBusinessReasons() {
        Account source = accounts.createAccount(new CreateAccountCommand("inactive-source", "EUR"));
        Account destination = accounts.createAccount(new CreateAccountCommand("active-destination", "EUR"));
        jdbcTemplate.update(
                "UPDATE accounts SET status='FROZEN' WHERE id=?", source.id().value());
        workflow.handle(initiated(
                UUID.randomUUID(), source.id().value(), destination.id().value(), "10.00", "EUR", "inactive"));
        assertThat(lastRejectionReason()).isEqualTo("SOURCE_ACCOUNT_INACTIVE");

        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM processed_events");
        jdbcTemplate.update("UPDATE accounts SET status='ACTIVE'");
        workflow.handle(initiated(
                UUID.randomUUID(), source.id().value(), destination.id().value(), "10.00", "USD", "currency-mismatch"));
        assertThat(lastRejectionReason()).isEqualTo("CURRENCY_MISMATCH");
    }

    private WorkflowEnvelope initiated(
            UUID transferId, Account source, Account destination, String amount, String reference) {
        return initiated(transferId, source.id().value(), destination.id().value(), amount, "EUR", reference);
    }

    private WorkflowEnvelope initiated(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            String amount,
            String currency,
            String reference) {
        return new WorkflowEnvelope(
                UUID.randomUUID(),
                AccountTransferWorkflowService.TRANSFER_INITIATED,
                1,
                Instant.parse("2026-07-23T12:00:00Z"),
                "correlation-1",
                "http-request",
                "transfer-service",
                mapper.valueToTree(Map.of(
                        "transferId",
                        transferId,
                        "sourceAccountId",
                        sourceAccountId,
                        "destinationAccountId",
                        destinationAccountId,
                        "amount",
                        amount,
                        "currency",
                        currency,
                        "reference",
                        reference)));
    }

    private WorkflowEnvelope command(String type, UUID transferId, String correlationId) {
        return new WorkflowEnvelope(
                UUID.randomUUID(),
                type,
                1,
                Instant.parse("2026-07-23T12:01:00Z"),
                correlationId,
                "risk-event",
                "transfer-service",
                mapper.valueToTree(Map.of("transferId", transferId)));
    }

    private String lastRejectionReason() {
        return jdbcTemplate.queryForObject(
                "SELECT payload #>> '{payload,reason}' FROM outbox_events ORDER BY occurred_at DESC LIMIT 1",
                String.class);
    }
}
