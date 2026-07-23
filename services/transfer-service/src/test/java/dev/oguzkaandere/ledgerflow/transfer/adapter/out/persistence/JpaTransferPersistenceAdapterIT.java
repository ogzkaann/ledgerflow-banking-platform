package dev.oguzkaandere.ledgerflow.transfer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.application.service.TransferApplicationService;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import dev.oguzkaandere.ledgerflow.transfer.support.TransferIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class JpaTransferPersistenceAdapterIT extends TransferIntegrationTest {
    @Autowired
    private TransferApplicationService application;

    @Autowired
    private SpringDataOutboxRepository outbox;

    @Test
    void databaseRejectsInvalidTransferValues() {
        UUID source = UUID.randomUUID();

        assertThatThrownBy(() -> insertTransfer(source, source, "1.00", "EUR", "PENDING", "reference"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertTransfer(source, UUID.randomUUID(), "0.00", "EUR", "PENDING", "reference"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertTransfer(source, UUID.randomUUID(), "1.00", "CHF", "PENDING", "reference"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertTransfer(source, UUID.randomUUID(), "1.00", "EUR", "UNKNOWN", "reference"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertTransfer(source, UUID.randomUUID(), "1.00", "EUR", "PENDING", " "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pendingOutboxQueryUsesDeterministicOldestFirstOrder() {
        create("outbox-order-1");
        create("outbox-order-2");
        create("outbox-order-3");

        var eventIds = jdbc.queryForList("SELECT event_id FROM outbox_events ORDER BY event_id", UUID.class);
        jdbc.update(
                "UPDATE outbox_events SET occurred_at = ? WHERE event_id = ?",
                OffsetDateTime.parse("2026-07-19T12:00:03Z"),
                eventIds.get(0));
        jdbc.update(
                "UPDATE outbox_events SET occurred_at = ? WHERE event_id = ?",
                OffsetDateTime.parse("2026-07-19T12:00:01Z"),
                eventIds.get(1));
        jdbc.update(
                "UPDATE outbox_events SET occurred_at = ? WHERE event_id = ?",
                OffsetDateTime.parse("2026-07-19T12:00:02Z"),
                eventIds.get(2));

        assertThat(outbox.findTop100ByStatusOrderByOccurredAtAscEventIdAsc("PENDING"))
                .extracting(event -> event.occurredAt)
                .containsExactly(
                        Instant.parse("2026-07-19T12:00:01Z"),
                        Instant.parse("2026-07-19T12:00:02Z"),
                        Instant.parse("2026-07-19T12:00:03Z"));
    }

    private void create(String key) {
        application.create(new CreateTransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Money(new BigDecimal("10.00"), SupportedCurrency.EUR),
                new TransferReference("outbox-order"),
                new IdempotencyKey(key),
                new CorrelationId(key)));
    }

    private void insertTransfer(
            UUID source, UUID destination, String amount, String currency, String status, String reference) {
        jdbc.update("""
                INSERT INTO transfers (
                    id, source_account_id, destination_account_id, amount, currency, reference,
                    status, correlation_id, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?::numeric, ?, ?, ?, 'constraint-test', 0, now(), now())
                """, UUID.randomUUID(), source, destination, amount, currency, reference, status);
    }
}
