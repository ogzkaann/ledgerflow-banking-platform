package dev.oguzkaandere.ledgerflow.transfer.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisIdempotencyCacheTest {
    @Test
    void degradesToCacheMissWhenRedisIsUnavailable() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(operations);
        when(operations.get("ledgerflow:idempotency:CREATE_TRANSFER:key"))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
        RedisIdempotencyCache cache = new RedisIdempotencyCache(template, Duration.ofHours(24));

        assertThat(cache.find("CREATE_TRANSFER", new IdempotencyKey("key"))).isEmpty();
    }

    @Test
    void usesExplicitStringRepresentation() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(operations);
        UUID id = UUID.randomUUID();
        when(operations.get("ledgerflow:idempotency:CREATE_TRANSFER:key")).thenReturn("abc:" + id);
        RedisIdempotencyCache cache = new RedisIdempotencyCache(template, Duration.ofHours(24));

        assertThat(cache.find("CREATE_TRANSFER", new IdempotencyKey("key")))
                .contains(new dev.oguzkaandere.ledgerflow.transfer.domain.port.IdempotencyCache.CachedIdempotency(
                        "abc", TransferId.from(id)));
    }
}
