package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web;

final class InvalidHeaderException extends RuntimeException {
    InvalidHeaderException(String message) {
        super(message);
    }
}
