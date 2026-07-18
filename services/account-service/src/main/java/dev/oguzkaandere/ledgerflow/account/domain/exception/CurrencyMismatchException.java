package dev.oguzkaandere.ledgerflow.account.domain.exception;

import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;

public final class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(SupportedCurrency expected, SupportedCurrency actual) {
        super("Expected currency " + expected + " but received " + actual);
    }
}
