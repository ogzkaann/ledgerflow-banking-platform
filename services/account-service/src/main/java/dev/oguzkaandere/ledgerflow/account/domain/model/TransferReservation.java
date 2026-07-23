package dev.oguzkaandere.ledgerflow.account.domain.model;

import dev.oguzkaandere.ledgerflow.account.domain.exception.InvalidReservationTransitionException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TransferReservation(
        UUID reservationId,
        UUID transferId,
        AccountId sourceAccountId,
        AccountId destinationAccountId,
        Money amount,
        ReservationStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public TransferReservation {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(destinationAccountId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Reservation accounts must be different");
        }
        if (amount.amount().signum() <= 0 || version < 0) {
            throw new IllegalArgumentException("Reservation amount must be positive and version non-negative");
        }
    }

    public static TransferReservation reserve(
            UUID reservationId,
            UUID transferId,
            AccountId sourceAccountId,
            AccountId destinationAccountId,
            Money amount,
            Instant now) {
        return new TransferReservation(
                reservationId,
                transferId,
                sourceAccountId,
                destinationAccountId,
                amount,
                ReservationStatus.RESERVED,
                0,
                now,
                now);
    }

    public TransferReservation settle(Instant now) {
        return transition(ReservationStatus.SETTLED, now);
    }

    public TransferReservation release(Instant now) {
        return transition(ReservationStatus.RELEASED, now);
    }

    private TransferReservation transition(ReservationStatus target, Instant now) {
        if (status != ReservationStatus.RESERVED) {
            throw new InvalidReservationTransitionException(
                    "Reservation %s cannot transition from %s to %s".formatted(transferId, status, target));
        }
        return new TransferReservation(
                reservationId,
                transferId,
                sourceAccountId,
                destinationAccountId,
                amount,
                target,
                version + 1,
                createdAt,
                now);
    }
}
