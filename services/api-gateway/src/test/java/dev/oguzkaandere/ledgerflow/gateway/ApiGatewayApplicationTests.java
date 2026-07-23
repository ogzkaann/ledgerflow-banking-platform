package dev.oguzkaandere.ledgerflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {
    @Autowired
    private RouteDefinitionLocator routes;

    @Test
    void contextLoads() {}

    @Test
    void exposesOnlyImplementedBusinessApis() {
        var definitions = routes.getRouteDefinitions().collectList().block();

        assertThat(definitions)
                .isNotNull()
                .extracting(definition -> definition.getId())
                .containsExactlyInAnyOrder("account-service", "transfer-service", "notification-service");
        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.getUri().getScheme()).isIn("http", "https");
            assertThat(definition.getPredicates()).hasSize(1);
            assertThat(definition.getPredicates().getFirst().getName()).isEqualTo("Path");
            assertThat(definition
                            .getPredicates()
                            .getFirst()
                            .getArgs()
                            .values()
                            .iterator()
                            .next())
                    .doesNotContain("actuator");
        });
    }
}
