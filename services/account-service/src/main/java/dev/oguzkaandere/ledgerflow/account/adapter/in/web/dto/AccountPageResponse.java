package dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto;

import java.util.List;

public record AccountPageResponse(
        List<AccountResponse> content, int page, int size, long totalElements, int totalPages) {}
