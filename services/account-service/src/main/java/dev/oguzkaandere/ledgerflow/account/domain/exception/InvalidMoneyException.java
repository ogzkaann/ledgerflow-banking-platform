package dev.oguzkaandere.ledgerflow.account.domain.exception;

public final class InvalidMoneyException extends RuntimeException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
