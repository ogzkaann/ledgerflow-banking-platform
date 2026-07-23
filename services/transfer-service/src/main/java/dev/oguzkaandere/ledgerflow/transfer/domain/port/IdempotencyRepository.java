package dev.oguzkaandere.ledgerflow.transfer.domain.port;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyRepository {
    void acquireCreationLock(String scope, IdempotencyKey key);

    Optional<IdempotencyRecord> find(String scope, IdempotencyKey key);

    IdempotencyRecord save(IdempotencyRecord record);
}
