package dev.oguzkaandere.ledgerflow.gateway;

import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(ServerHttpSecurity http, MeterRegistry meters) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .pathMatchers("/actuator/health/liveness", "/actuator/health/readiness")
                        .permitAll()
                        .pathMatchers("/actuator/prometheus", "/actuator/metrics/**", "/actuator/info")
                        .hasRole("ledgerflow-admin")
                        .pathMatchers(HttpMethod.POST, "/api/v1/accounts/*/test-funding")
                        .hasRole("ledgerflow-admin")
                        .pathMatchers(HttpMethod.POST, "/api/v1/accounts", "/api/v1/transfers")
                        .hasAnyRole("ledgerflow-operator", "ledgerflow-admin")
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/accounts/**",
                                "/api/v1/transfers/**",
                                "/api/v1/notifications/**")
                        .hasAnyRole("ledgerflow-operator", "ledgerflow-auditor", "ledgerflow-admin")
                        .anyExchange()
                        .denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(realmRoles())))
                        .authenticationEntryPoint((exchange, exception) -> {
                            meters.counter("security.authentication.failures", "service", "api-gateway")
                                    .increment();
                            return problem(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized");
                        }))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((exchange, exception) -> {
                    meters.counter("security.authorization.denials", "service", "api-gateway")
                            .increment();
                    return problem(exchange, HttpStatus.FORBIDDEN, "Forbidden");
                }))
                .headers(Customizer.withDefaults())
                .build();
    }

    @Bean
    KeyResolver authenticatedPrincipalKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(Authentication.class)
                .map(authentication -> {
                    if (authentication.getPrincipal() instanceof Jwt jwt) {
                        String clientId = jwt.getClaimAsString("azp");
                        return "gateway-rate-limit:principal:"
                                + (clientId == null || clientId.isBlank()
                                        ? jwt.getSubject()
                                        : clientId + ":" + jwt.getSubject());
                    }
                    return "gateway-rate-limit:principal:" + authentication.getName();
                })
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    var address = exchange.getRequest().getRemoteAddress();
                    String host =
                            address == null ? "unknown" : address.getAddress().getHostAddress();
                    return "gateway-rate-limit:ip:" + host;
                }));
    }

    private static Converter<Jwt, AbstractAuthenticationToken> realmRoles() {
        return jwt -> new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
    }

    private static Collection<SimpleGrantedAuthority> authorities(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access) || !(access.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> role.startsWith("ledgerflow-"))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    private static Mono<Void> problem(
            org.springframework.web.server.ServerWebExchange exchange, HttpStatus status, String title) {
        byte[] body = ("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status.value() + "}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
