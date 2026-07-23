package dev.oguzkaandere.ledgerflow.transfer.domain.exception;

public final class TransferNotFoundException extends TransferDomainException {
    public TransferNotFoundException(String id) {
        super("Transfer '%s' was not found".formatted(id));
    }
}
