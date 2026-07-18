package dev.oguzkaandere.ledgerflow.account.domain.model;

import java.util.List;

public record LedgerPage(List<LedgerEntry> content, int page, int size, long totalElements) {

    public LedgerPage {
        content = List.copyOf(content);
        if (page < 0 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("Invalid ledger pagination metadata");
        }
    }

    public int totalPages() {
        return totalElements == 0 ? 0 : Math.toIntExact((totalElements + size - 1) / size);
    }
}
