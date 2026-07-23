package dev.oguzkaandere.ledgerflow.transfer.domain.port;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStateTransition;
import java.util.List;

public interface TransferHistoryRepository {
    TransferStateTransition save(TransferStateTransition transition);

    List<TransferStateTransition> findByTransferIdOldestFirst(TransferId transferId);
}
