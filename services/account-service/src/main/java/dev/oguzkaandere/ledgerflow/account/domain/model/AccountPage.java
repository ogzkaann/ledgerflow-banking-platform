package dev.oguzkaandere.ledgerflow.account.domain.model;

import java.util.List;

public record AccountPage(List<Account> content, int page, int size, long totalElements, int totalPages) {

    public AccountPage {
        content = List.copyOf(content);
        if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Invalid account page metadata");
        }
    }
}
