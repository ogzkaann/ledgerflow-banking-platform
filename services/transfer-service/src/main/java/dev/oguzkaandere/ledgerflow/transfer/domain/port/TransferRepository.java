package dev.oguzkaandere.ledgerflow.transfer.domain.port;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import java.util.Optional;

public interface TransferRepository {
    Transfer save(Transfer transfer);

    Optional<Transfer> findById(TransferId id);

    Optional<Transfer> findByIdForUpdate(TransferId id);
}
