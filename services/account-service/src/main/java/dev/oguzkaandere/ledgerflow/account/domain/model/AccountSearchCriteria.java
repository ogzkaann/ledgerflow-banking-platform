package dev.oguzkaandere.ledgerflow.account.domain.model;

public record AccountSearchCriteria(int page, int size, AccountStatus status, SupportedCurrency currency) {

    public AccountSearchCriteria {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
    }
}
