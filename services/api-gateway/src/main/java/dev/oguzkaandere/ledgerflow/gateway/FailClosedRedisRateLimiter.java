package dev.oguzkaandere.ledgerflow.gateway;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Primary
@Component("redisRateLimiter")
final class FailClosedRedisRateLimiter extends RedisRateLimiter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FailClosedRedisRateLimiter.class);

    private final MeterRegistry meterRegistry;

    FailClosedRedisRateLimiter(
            ReactiveStringRedisTemplate redisTemplate,
            @Qualifier(REDIS_SCRIPT_NAME) RedisScript<List<Long>> redisScript,
            ConfigurationService configurationService,
            MeterRegistry meterRegistry) {
        super(redisTemplate, redisScript, configurationService);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<RateLimiter.Response> isAllowed(String routeId, String id) {
        try {
            return super.isAllowed(routeId, id)
                    .map(response -> backendUnavailable(response, getRemainingHeader())
                            ? deny(routeId, response, null)
                            : response)
                    .onErrorResume(exception -> Mono.just(deny(routeId, null, exception)));
        } catch (RuntimeException exception) {
            return Mono.just(deny(routeId, null, exception));
        }
    }

    static boolean backendUnavailable(RateLimiter.Response response, String remainingHeader) {
        return response.isAllowed() && "-1".equals(response.getHeaders().get(remainingHeader));
    }

    private RateLimiter.Response deny(String routeId, RateLimiter.Response response, Throwable exception) {
        meterRegistry
                .counter("gateway.rate_limit.backend.failures", "route", routeId)
                .increment();
        if (exception == null) {
            LOGGER.warn("gateway_rate_limiter_backend_unavailable routeId={}", routeId);
        } else {
            LOGGER.warn(
                    "gateway_rate_limiter_backend_unavailable routeId={} exceptionType={}",
                    routeId,
                    exception.getClass().getSimpleName());
        }

        var headers = new LinkedHashMap<String, String>();
        if (response != null) {
            headers.putAll(response.getHeaders());
        }
        headers.put(getRemainingHeader(), "0");
        return new RateLimiter.Response(false, headers);
    }
}
