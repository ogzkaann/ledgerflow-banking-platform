package dev.oguzkaandere.ledgerflow.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.account.domain.exception.InvalidReservationTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferReservationTest {
    @Test
    void permitsOneSettlementOrReleaseOnly() {
        TransferReservation reservation = reservation();
        assertThat(reservation.settle(Instant.parse("2026-07-23T12:01:00Z")).status())
                .isEqualTo(ReservationStatus.SETTLED);
        assertThat(reservation.release(Instant.parse("2026-07-23T12:01:00Z")).status())
                .isEqualTo(ReservationStatus.RELEASED);
        assertThatThrownBy(() -> reservation
                        .settle(Instant.parse("2026-07-23T12:01:00Z"))
                        .release(Instant.parse("2026-07-23T12:02:00Z")))
                .isInstanceOf(InvalidReservationTransitionException.class);
        assertThatThrownBy(() -> reservation
                        .release(Instant.parse("2026-07-23T12:01:00Z"))
                        .settle(Instant.parse("2026-07-23T12:02:00Z")))
                .isInstanceOf(InvalidReservationTransitionException.class);
    }

    private static TransferReservation reservation() {
        return TransferReservation.reserve(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AccountId.from(UUID.randomUUID()),
                AccountId.from(UUID.randomUUID()),
                Money.positive(new BigDecimal("125.50"), SupportedCurrency.EUR),
                Instant.parse("2026-07-23T12:00:00Z"));
    }
}
