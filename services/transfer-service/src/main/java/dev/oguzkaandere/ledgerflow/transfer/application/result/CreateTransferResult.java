package dev.oguzkaandere.ledgerflow.transfer.application.result;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;

public record CreateTransferResult(Transfer transfer, boolean replayed) {}
