package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        String amount,
        String currency,
        String reference,
        String status,
        String correlationId,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
