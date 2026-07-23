package dev.oguzkaandere.ledgerflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

class FailClosedRedisRateLimiterTest {

    @Test
    void recognizesOnlyTheRedisBackendFailureSentinel() {
        assertThat(FailClosedRedisRateLimiter.backendUnavailable(
                        new RateLimiter.Response(true, Map.of(RedisRateLimiter.REMAINING_HEADER, "-1")),
                        RedisRateLimiter.REMAINING_HEADER))
                .isTrue();
        assertThat(FailClosedRedisRateLimiter.backendUnavailable(
                        new RateLimiter.Response(true, Map.of(RedisRateLimiter.REMAINING_HEADER, "2")),
                        RedisRateLimiter.REMAINING_HEADER))
                .isFalse();
        assertThat(FailClosedRedisRateLimiter.backendUnavailable(
                        new RateLimiter.Response(false, Map.of(RedisRateLimiter.REMAINING_HEADER, "-1")),
                        RedisRateLimiter.REMAINING_HEADER))
                .isFalse();
    }
}
