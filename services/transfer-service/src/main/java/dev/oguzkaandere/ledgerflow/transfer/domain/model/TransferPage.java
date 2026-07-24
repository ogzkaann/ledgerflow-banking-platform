package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.util.List;

public record TransferPage(List<Transfer> content, int page, int size, long totalElements, int totalPages) {

    public TransferPage {
        content = List.copyOf(content);
        if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Invalid transfer page metadata");
        }
    }
}
