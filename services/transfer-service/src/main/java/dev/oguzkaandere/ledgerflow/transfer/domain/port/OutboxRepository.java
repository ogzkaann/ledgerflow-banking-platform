package dev.oguzkaandere.ledgerflow.transfer.domain.port;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferInitiatedEvent;

public interface OutboxRepository {
    void save(TransferInitiatedEvent event);
}
