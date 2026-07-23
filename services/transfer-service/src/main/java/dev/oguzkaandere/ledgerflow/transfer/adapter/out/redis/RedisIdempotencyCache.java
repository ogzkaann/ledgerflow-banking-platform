package dev.oguzkaandere.ledgerflow.transfer.adapter.out.redis;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.IdempotencyCache;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class RedisIdempotencyCache implements IdempotencyCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisIdempotencyCache.class);

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisIdempotencyCache(
            StringRedisTemplate redis, @Value("${ledgerflow.transfer.idempotency.redis-ttl:24h}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public Optional<CachedIdempotency> find(String scope, IdempotencyKey key) {
        try {
            String value = redis.opsForValue().get(redisKey(scope, key));
            if (value == null) {
                return Optional.empty();
            }
            String[] fields = value.split(":", 2);
            if (fields.length != 2) {
                return Optional.empty();
            }
            return Optional.of(new CachedIdempotency(fields[0], TransferId.from(UUID.fromString(fields[1]))));
        } catch (RuntimeException exception) {
            warnFallback(exception);
            return Optional.empty();
        }
    }

    @Override
    public void put(String scope, IdempotencyKey key, String fingerprint, TransferId transferId) {
        try {
            redis.opsForValue().set(redisKey(scope, key), fingerprint + ":" + transferId, ttl);
        } catch (RuntimeException exception) {
            warnFallback(exception);
        }
    }

    private static String redisKey(String scope, IdempotencyKey key) {
        return "ledgerflow:idempotency:" + scope + ":" + key.value();
    }

    private static void warnFallback(Exception exception) {
        LOGGER.warn(
                "redis_unavailable_postgresql_fallback exceptionType={}",
                exception.getClass().getSimpleName());
    }
}
