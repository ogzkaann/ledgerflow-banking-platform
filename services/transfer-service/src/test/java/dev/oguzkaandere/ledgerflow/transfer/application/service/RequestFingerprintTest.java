package dev.oguzkaandere.ledgerflow.transfer.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {
    @Test
    void ignoresEquivalentAmountFormattingAndCorrelationMetadata() {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        String first = RequestFingerprint.forCommand(command(source, destination, "125.5", "correlation-a"));
        String second = RequestFingerprint.forCommand(command(source, destination, "125.50", "correlation-b"));
        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    void changesWhenLogicalRequestChanges() {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        assertThat(RequestFingerprint.forCommand(command(source, destination, "125.50", "correlation-a")))
                .isNotEqualTo(RequestFingerprint.forCommand(command(source, destination, "126.00", "correlation-a")));
    }

    private static CreateTransferCommand command(UUID source, UUID destination, String amount, String correlation) {
        return new CreateTransferCommand(
                source,
                destination,
                new Money(new BigDecimal(amount), SupportedCurrency.EUR),
                new TransferReference(" invoice-1 "),
                new IdempotencyKey("key-1"),
                new CorrelationId(correlation));
    }
}
