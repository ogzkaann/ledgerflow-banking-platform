package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.ReservationStatus;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.account.domain.model.TransferReservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_reservations")
class TransferReservationJpaEntity {
    @Id
    @Column(name = "reservation_id")
    UUID reservationId;

    @Column(name = "transfer_id", nullable = false, unique = true)
    UUID transferId;

    @Column(name = "source_account_id", nullable = false)
    UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    UUID destinationAccountId;

    @Column(nullable = false, precision = Money.PRECISION, scale = Money.SCALE)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    SupportedCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ReservationStatus status;

    @Version
    @Column(nullable = false)
    long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected TransferReservationJpaEntity() {}

    static TransferReservationJpaEntity fromDomain(TransferReservation value) {
        TransferReservationJpaEntity entity = new TransferReservationJpaEntity();
        entity.reservationId = value.reservationId();
        entity.transferId = value.transferId();
        entity.sourceAccountId = value.sourceAccountId().value();
        entity.destinationAccountId = value.destinationAccountId().value();
        entity.amount = value.amount().amount();
        entity.currency = value.amount().currency();
        entity.status = value.status();
        entity.version = value.version();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }

    void updateFrom(TransferReservation value) {
        status = value.status();
        updatedAt = value.updatedAt();
    }

    TransferReservation toDomain() {
        return new TransferReservation(
                reservationId,
                transferId,
                AccountId.from(sourceAccountId),
                AccountId.from(destinationAccountId),
                new Money(amount, currency),
                status,
                version,
                createdAt,
                updatedAt);
    }
}
