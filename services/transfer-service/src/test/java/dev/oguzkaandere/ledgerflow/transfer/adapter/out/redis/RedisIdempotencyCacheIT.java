package dev.oguzkaandere.ledgerflow.transfer.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisIdempotencyCacheIT {
    @Test
    void realRedisSupportsStringCacheTtlAndSafeOutage() {
        try (var redisContainer = new GenericContainer<>(DockerImageName.parse("redis:8.8.0")).withExposedPorts(6379)) {
            redisContainer.start();
            var configuration =
                    new RedisStandaloneConfiguration(redisContainer.getHost(), redisContainer.getMappedPort(6379));
            var clientConfiguration = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofMillis(500))
                    .shutdownTimeout(Duration.ZERO)
                    .build();
            var factory = new LettuceConnectionFactory(configuration, clientConfiguration);
            factory.afterPropertiesSet();
            factory.start();
            try {
                var cache = new RedisIdempotencyCache(new StringRedisTemplate(factory), Duration.ofHours(24));
                IdempotencyKey key = new IdempotencyKey("integration-key");
                TransferId transferId = TransferId.from(UUID.randomUUID());
                cache.put("CREATE_TRANSFER", key, "fingerprint", transferId);

                assertThat(cache.find("CREATE_TRANSFER", key))
                        .contains(
                                new dev.oguzkaandere.ledgerflow.transfer.domain.port.IdempotencyCache.CachedIdempotency(
                                        "fingerprint", transferId));

                redisContainer.stop();
                assertThat(cache.find("CREATE_TRANSFER", key)).isEmpty();
            } finally {
                factory.destroy();
            }
        }
    }
}
