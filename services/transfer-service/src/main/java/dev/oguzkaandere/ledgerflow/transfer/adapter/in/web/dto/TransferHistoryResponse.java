package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record TransferHistoryResponse(
        UUID transitionId, String fromStatus, String toStatus, String reason, Instant occurredAt, long sequence) {}
