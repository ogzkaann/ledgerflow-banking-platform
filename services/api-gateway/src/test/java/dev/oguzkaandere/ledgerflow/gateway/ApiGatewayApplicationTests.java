package dev.oguzkaandere.ledgerflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {
    @Autowired
    private RouteDefinitionLocator routes;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RedisRateLimiter rateLimiter;

    private WebTestClient client;

    @BeforeEach
    void setUpClient() {
        client = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void contextLoadsWithFailClosedRateLimiter() {
        assertThat(rateLimiter).isInstanceOf(FailClosedRedisRateLimiter.class);
    }

    @Test
    void exposesOnlyImplementedBusinessApis() {
        var definitions = routes.getRouteDefinitions().collectList().block();

        assertThat(definitions)
                .isNotNull()
                .extracting(definition -> definition.getId())
                .containsExactlyInAnyOrder(
                        "account-create",
                        "account-funding",
                        "transfer-create",
                        "account-read",
                        "transfer-read",
                        "notification-read");
        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.getUri().getScheme()).isIn("http", "https");
            assertThat(definition.getPredicates()).isNotEmpty();
            assertThat(definition.getPredicates().stream().flatMap(predicate -> predicate.getArgs().values().stream()))
                    .noneMatch(value -> value.contains("actuator") || value.contains("risk"));
        });
    }

    @Test
    void rejectsMissingTokenWithSafeProblemResponse() {
        client.post()
                .uri("/api/v1/accounts")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(401);
    }

    @Test
    void rejectsAuditorWriteBeforeRouting() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ledgerflow-auditor")))
                .post()
                .uri("/api/v1/accounts")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void keepsProbesPublicAndReplacesInvalidCorrelationId() {
        client.get()
                .uri("/actuator/health/liveness")
                .header(CorrelationGatewayFilter.CORRELATION_HEADER, "invalid value with spaces")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueMatches(CorrelationGatewayFilter.CORRELATION_HEADER, "[0-9a-f-]{36}")
                .expectHeader()
                .valueEquals(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    @Test
    void deniesUnapprovedCorsOrigin() {
        client.options()
                .uri("/api/v1/accounts")
                .header(HttpHeaders.ORIGIN, "https://attacker.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
