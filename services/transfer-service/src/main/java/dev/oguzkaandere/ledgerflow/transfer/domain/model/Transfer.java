package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferTransitionException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Transfer {
    private static final Map<TransferStatus, Set<TransferStatus>> LEGAL_TRANSITIONS = Map.of(
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

    private final TransferId id;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final Money money;
    private final TransferReference reference;
    private final TransferStatus status;
    private final CorrelationId correlationId;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Transfer(
            TransferId id,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money money,
            TransferReference reference,
            TransferStatus status,
            CorrelationId correlationId,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId);
        this.destinationAccountId = Objects.requireNonNull(destinationAccountId);
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new InvalidTransferException("Source and destination accounts must be different");
        }
        this.money = Objects.requireNonNull(money);
        this.reference = Objects.requireNonNull(reference);
        this.status = Objects.requireNonNull(status);
        this.correlationId = Objects.requireNonNull(correlationId);
        if (version < 0) {
            throw new InvalidTransferException("Transfer version cannot be negative");
        }
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Transfer pending(
            TransferId id,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money money,
            TransferReference reference,
            CorrelationId correlationId,
            Instant now) {
        return new Transfer(
                id,
                sourceAccountId,
                destinationAccountId,
                money,
                reference,
                TransferStatus.PENDING,
                correlationId,
                0,
                now,
                now);
    }

    public Transition transitionTo(TransferStatus target, String reason, Instant now, UUID transitionId) {
        if (target == status) {
            throw new InvalidTransferTransitionException("Repeated transition to current status is rejected");
        }
        if (status.terminal()
                || !LEGAL_TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new InvalidTransferTransitionException(
                    "Transition from %s to %s is not allowed".formatted(status, target));
        }
        Transfer changed = new Transfer(
                id,
                sourceAccountId,
                destinationAccountId,
                money,
                reference,
                target,
                correlationId,
                version + 1,
                createdAt,
                now);
        return new Transition(
                changed, new TransferStateTransition(transitionId, id, status, target, reason, now, version + 1));
    }

    public TransferId id() {
        return id;
    }

    public UUID sourceAccountId() {
        return sourceAccountId;
    }

    public UUID destinationAccountId() {
        return destinationAccountId;
    }

    public Money money() {
        return money;
    }

    public TransferReference reference() {
        return reference;
    }

    public TransferStatus status() {
        return status;
    }

    public CorrelationId correlationId() {
        return correlationId;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public record Transition(Transfer transfer, TransferStateTransition history) {}
}
