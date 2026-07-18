package dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto;

import java.util.List;

public record LedgerPageResponse(
        List<LedgerEntryResponse> content, int page, int size, long totalElements, int totalPages) {

    public LedgerPageResponse {
        content = List.copyOf(content);
    }
}
