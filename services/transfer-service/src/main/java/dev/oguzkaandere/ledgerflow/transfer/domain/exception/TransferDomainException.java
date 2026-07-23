package dev.oguzkaandere.ledgerflow.transfer.domain.exception;

public abstract class TransferDomainException extends RuntimeException {
    protected TransferDomainException(String message) {
        super(message);
    }
}
