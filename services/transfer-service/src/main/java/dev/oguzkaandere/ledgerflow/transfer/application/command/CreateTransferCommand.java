package dev.oguzkaandere.ledgerflow.transfer.application.command;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import java.util.UUID;

public record CreateTransferCommand(
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money money,
        TransferReference reference,
        IdempotencyKey idempotencyKey,
        CorrelationId correlationId) {}
