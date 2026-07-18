package dev.oguzkaandere.ledgerflow.account.adapter.in.web;

final class InvalidRequestException extends RuntimeException {

    InvalidRequestException(String message) {
        super(message);
    }
}
