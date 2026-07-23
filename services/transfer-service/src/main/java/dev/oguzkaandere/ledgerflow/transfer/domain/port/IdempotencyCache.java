package dev.oguzkaandere.ledgerflow.transfer.domain.port;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import java.util.Optional;

public interface IdempotencyCache {
    Optional<CachedIdempotency> find(String scope, IdempotencyKey key);

    void put(String scope, IdempotencyKey key, String fingerprint, TransferId transferId);

    record CachedIdempotency(String fingerprint, TransferId transferId) {}
}
