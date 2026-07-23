package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");
    private static final Map<TransferStatus, Set<TransferStatus>> LEGAL = Map.of(
            TransferStatus.PENDING,
            Set.of(TransferStatus.FUNDS_RESERVED, TransferStatus.REJECTED, TransferStatus.EXPIRED),
            TransferStatus.FUNDS_RESERVED,
            Set.of(TransferStatus.RISK_APPROVED, TransferStatus.REJECTED, TransferStatus.COMPENSATING),
            TransferStatus.RISK_APPROVED,
            Set.of(TransferStatus.SETTLING, TransferStatus.COMPENSATING),
            TransferStatus.SETTLING,
            Set.of(TransferStatus.COMPLETED, TransferStatus.COMPENSATING),
            TransferStatus.COMPENSATING,
            Set.of(TransferStatus.REJECTED));

    @Test
    void createsPendingTransferWithNormalizedMoney() {
        Transfer transfer = transfer(TransferStatus.PENDING);
        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.money().canonicalAmount()).isEqualTo("125.50");
        assertThat(transfer.version()).isZero();
    }

    @Test
    void rejectsSameAccountsAndInvalidMoney() {
        UUID account = UUID.randomUUID();
        assertThatThrownBy(() -> Transfer.pending(
                        TransferId.from(UUID.randomUUID()),
                        account,
                        account,
                        new Money(new BigDecimal("1"), SupportedCurrency.EUR),
                        new TransferReference("ref"),
                        new CorrelationId("corr"),
                        NOW))
                .isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001"), SupportedCurrency.EUR))
                .isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ZERO, SupportedCurrency.EUR))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void enforcesCompleteTransitionMatrixAndCreatesHistory() {
        for (TransferStatus from : TransferStatus.values()) {
            for (TransferStatus to : TransferStatus.values()) {
                Transfer transfer = transfer(from);
                if (LEGAL.getOrDefault(from, Set.of()).contains(to)) {
                    Transfer.Transition result =
                            transfer.transitionTo(to, "EVENT", NOW.plusSeconds(1), UUID.randomUUID());
                    assertThat(result.transfer().status()).isEqualTo(to);
                    assertThat(result.transfer().version()).isEqualTo(1);
                    assertThat(result.history().fromStatus()).isEqualTo(from);
                    assertThat(result.history().toStatus()).isEqualTo(to);
                } else {
                    assertThatThrownBy(() -> transfer.transitionTo(to, "EVENT", NOW.plusSeconds(1), UUID.randomUUID()))
                            .isInstanceOf(InvalidTransferTransitionException.class);
                }
            }
        }
    }

    private static Transfer transfer(TransferStatus status) {
        return new Transfer(
                TransferId.from(UUID.randomUUID()),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Money(new BigDecimal("125.5"), SupportedCurrency.EUR),
                new TransferReference("invoice"),
                status,
                new CorrelationId("corr-1"),
                0,
                NOW,
                NOW);
    }
}
