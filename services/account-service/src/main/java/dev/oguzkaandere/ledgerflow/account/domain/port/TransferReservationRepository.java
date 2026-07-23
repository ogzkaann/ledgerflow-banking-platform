package dev.oguzkaandere.ledgerflow.account.domain.port;

import dev.oguzkaandere.ledgerflow.account.domain.model.TransferReservation;
import java.util.Optional;
import java.util.UUID;

public interface TransferReservationRepository {
    TransferReservation save(TransferReservation reservation);

    Optional<TransferReservation> findByTransferIdForUpdate(UUID transferId);
}
