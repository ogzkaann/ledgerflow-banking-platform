package dev.oguzkaandere.ledgerflow.transfer.domain.exception;

public final class IdempotencyConflictException extends TransferDomainException {
    public IdempotencyConflictException() {
        super("The idempotency key was already used with a different transfer request");
    }
}
