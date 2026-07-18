package dev.oguzkaandere.ledgerflow.account.domain.exception;

public final class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currencyCode) {
        super("Currency " + currencyCode + " is not supported; supported currencies are EUR, USD, and GBP");
    }
}
