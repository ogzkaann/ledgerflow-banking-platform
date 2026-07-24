package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto;

import java.util.List;

public record TransferPageResponse(
        List<TransferResponse> content, int page, int size, long totalElements, int totalPages) {}
