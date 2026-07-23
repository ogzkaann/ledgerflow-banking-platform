package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.account.domain.model.TransferReservation;
import dev.oguzkaandere.ledgerflow.account.domain.port.TransferReservationRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaTransferReservationRepositoryAdapter implements TransferReservationRepository {
    private final SpringDataTransferReservationRepository repository;

    JpaTransferReservationRepositoryAdapter(SpringDataTransferReservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public TransferReservation save(TransferReservation reservation) {
        TransferReservationJpaEntity entity =
                repository.findById(reservation.reservationId()).orElse(null);
        if (entity == null) {
            entity = TransferReservationJpaEntity.fromDomain(reservation);
        } else {
            entity.updateFrom(reservation);
        }
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<TransferReservation> findByTransferIdForUpdate(UUID transferId) {
        return repository.findByTransferIdForUpdate(transferId).map(TransferReservationJpaEntity::toDomain);
    }
}
