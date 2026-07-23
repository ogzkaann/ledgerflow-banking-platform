package dev.oguzkaandere.ledgerflow.transfer.domain.port;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferInitiatedEvent;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.WorkflowOutboxEvent;

public interface OutboxRepository {
    void save(TransferInitiatedEvent event);

    void save(WorkflowOutboxEvent event);
}
