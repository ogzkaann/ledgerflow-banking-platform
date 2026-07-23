package dev.oguzkaandere.ledgerflow.gateway;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorrelationGatewayFilter implements WebFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}");
    private static final String[] UNTRUSTED_IDENTITY_HEADERS = {
        "X-Authenticated-User", "X-User-Id", "X-User-Roles", "X-Forwarded-User"
    };
    private final MeterRegistry meters;

    public CorrelationGatewayFilter(MeterRegistry meters) {
        this.meters = meters;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String supplied = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        String correlationId = isValid(supplied) ? supplied : UUID.randomUUID().toString();

        var request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.set(CORRELATION_HEADER, correlationId);
                    for (String header : UNTRUSTED_IDENTITY_HEADERS) {
                        headers.remove(header);
                    }
                })
                .build();
        exchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        exchange.getResponse().getHeaders().set("Pragma", "no-cache");
        exchange.getResponse().getHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponse().getHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().set(HttpHeaders.VARY, "Origin");
        exchange.getResponse().beforeCommit(() -> {
            if (exchange.getResponse().getStatusCode() != null
                    && exchange.getResponse().getStatusCode().value() == 429) {
                if (!exchange.getResponse().getHeaders().containsHeader(HttpHeaders.RETRY_AFTER)) {
                    exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "1");
                }
                meters.counter("gateway.rate.limit.rejections", "service", "api-gateway")
                        .increment();
            }
            return Mono.empty();
        });

        return Mono.defer(() -> {
                    MDC.put("correlationId", correlationId);
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .doFinally(signal -> MDC.remove("correlationId"));
    }

    static boolean isValid(String value) {
        return value != null && SAFE_CORRELATION_ID.matcher(value).matches();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
