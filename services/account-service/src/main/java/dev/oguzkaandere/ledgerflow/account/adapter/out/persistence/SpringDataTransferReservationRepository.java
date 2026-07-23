package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataTransferReservationRepository extends JpaRepository<TransferReservationJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from TransferReservationJpaEntity reservation where reservation.transferId = :id")
    Optional<TransferReservationJpaEntity> findByTransferIdForUpdate(@Param("id") UUID transferId);
}
