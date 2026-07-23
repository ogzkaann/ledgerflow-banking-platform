package dev.oguzkaandere.ledgerflow.account.domain.exception;

public class InvalidReservationTransitionException extends RuntimeException {
    public InvalidReservationTransitionException(String message) {
        super(message);
    }
}
