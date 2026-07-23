package dev.oguzkaandere.ledgerflow.transfer.support;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TransferIntegrationTest {
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4")
            .withDatabaseName("ledgerflow_transfer_test")
            .withUsername("ledgerflow_test")
            .withPassword("ledgerflow_test");
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8.8.0")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("ledgerflow.kafka.listener-enabled", () -> "false");
        registry.add("ledgerflow.outbox.scheduling-enabled", () -> "false");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StringRedisTemplate redis;

    @BeforeEach
    void cleanStores() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM processed_events");
        jdbc.update("DELETE FROM idempotency_records");
        jdbc.update("DELETE FROM transfer_state_history");
        jdbc.update("DELETE FROM transfers");
        Set<String> keys = redis.keys("ledgerflow:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
