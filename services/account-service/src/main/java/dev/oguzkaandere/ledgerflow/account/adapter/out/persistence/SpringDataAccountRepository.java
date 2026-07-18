package dev.oguzkaandere.ledgerflow.account.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountJpaEntity account where account.id = :accountId")
    Optional<AccountJpaEntity> findByIdForUpdate(@Param("accountId") UUID accountId);
}
