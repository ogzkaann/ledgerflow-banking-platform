package dev.oguzkaandere.ledgerflow.notification.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import dev.oguzkaandere.ledgerflow.account.AccountServiceApplication;
import dev.oguzkaandere.ledgerflow.account.application.command.CreateAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.command.FundAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.service.AccountApplicationService;
import dev.oguzkaandere.ledgerflow.notification.NotificationServiceApplication;
import dev.oguzkaandere.ledgerflow.notification.application.service.NotificationWorkflowService;
import dev.oguzkaandere.ledgerflow.risk.RiskServiceApplication;
import dev.oguzkaandere.ledgerflow.transfer.TransferServiceApplication;
import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.application.service.TransferApplicationService;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaWorkflowE2EIT {
    private final PostgreSQLContainer accountDb = postgres("account");
    private final PostgreSQLContainer transferDb = postgres("transfer");
    private final PostgreSQLContainer riskDb = postgres("risk");
    private final PostgreSQLContainer notificationDb = postgres("notification");
    private final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:8.8.0")).withExposedPorts(6379);
    private final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.2");

    private ConfigurableApplicationContext accountContext;
    private ConfigurableApplicationContext transferContext;
    private ConfigurableApplicationContext riskContext;
    private ConfigurableApplicationContext notificationContext;

    @BeforeAll
    void startWorkflow() {
        Startables.deepStart(Stream.of(accountDb, transferDb, riskDb, notificationDb, redis, kafka))
                .join();
        String bootstrap = kafka.getBootstrapServers();
        accountContext = context(
                AccountServiceApplication.class,
                database(accountDb),
                Map.of(
                        "spring.flyway.locations",
                        "classpath:db/migration/account",
                        "spring.kafka.bootstrap-servers",
                        bootstrap));
        transferContext = context(
                TransferServiceApplication.class,
                database(transferDb),
                Map.of(
                        "spring.flyway.locations",
                        "classpath:db/migration/transfer",
                        "spring.kafka.bootstrap-servers",
                        bootstrap,
                        "spring.data.redis.host",
                        redis.getHost(),
                        "spring.data.redis.port",
                        redis.getMappedPort(6379)));
        riskContext = context(
                RiskServiceApplication.class,
                database(riskDb),
                Map.of(
                        "spring.flyway.locations",
                        "classpath:db/migration/risk",
                        "spring.kafka.bootstrap-servers",
                        bootstrap));
        notificationContext = context(
                NotificationServiceApplication.class,
                database(notificationDb),
                Map.of(
                        "spring.flyway.locations",
                        "classpath:db/migration/notification",
                        "spring.kafka.bootstrap-servers",
                        bootstrap));
    }

    @AfterAll
    void stopWorkflow() {
        Stream.of(notificationContext, riskContext, transferContext, accountContext)
                .filter(java.util.Objects::nonNull)
                .forEach(ConfigurableApplicationContext::close);
        kafka.stop();
        redis.stop();
        notificationDb.stop();
        riskDb.stop();
        transferDb.stop();
        accountDb.stop();
    }

    @Test
    void happyPathMovesMoneyCompletesTransferAndNotifiesOnce() {
        AccountApplicationService accounts = accountContext.getBean(AccountApplicationService.class);
        TransferApplicationService transfers = transferContext.getBean(TransferApplicationService.class);
        NotificationWorkflowService notifications = notificationContext.getBean(NotificationWorkflowService.class);

        var source = accounts.createAccount(new CreateAccountCommand("e2e-source", "EUR"));
        var destination = accounts.createAccount(new CreateAccountCommand("e2e-destination", "EUR"));
        accounts.addTestFunding(new FundAccountCommand(source.id(), new BigDecimal("1000.00"), "e2e-initial-funding"));
        accounts.addTestFunding(
                new FundAccountCommand(destination.id(), new BigDecimal("100.00"), "e2e-destination-funding"));
        var transfer = transfers
                .create(new CreateTransferCommand(
                        source.id().value(),
                        destination.id().value(),
                        new Money(new BigDecimal("125.50"), SupportedCurrency.EUR),
                        new TransferReference("e2e-approved"),
                        new IdempotencyKey("e2e-approved-key"),
                        new CorrelationId("e2e-approved-correlation")))
                .transfer();

        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> assertThat(transfers.getTransfer(transfer.id()).status())
                                .isEqualTo(TransferStatus.COMPLETED));

        var finalSource = accounts.getAccount(source.id());
        var finalDestination = accounts.getAccount(destination.id());
        assertThat(finalSource.availableBalance().formattedAmount()).isEqualTo("874.50");
        assertThat(finalSource.reservedBalance().formattedAmount()).isEqualTo("0.00");
        assertThat(finalDestination.availableBalance().formattedAmount()).isEqualTo("225.50");
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(
                                notifications.findByTransferId(transfer.id().value()))
                        .hasSize(1));
        assertThat(accountContext
                        .getBean(JdbcTemplate.class)
                        .queryForObject(
                                "SELECT count(*) FROM ledger_entries WHERE reference LIKE ?",
                                Integer.class,
                                "transfer:" + transfer.id() + ":%"))
                .isEqualTo(2);
        assertOutboxesPublished();
    }

    @Test
    void duplicateKafkaEventDoesNotRepeatMoneyMovementOrWorkflowEffects() {
        AccountApplicationService accounts = accountContext.getBean(AccountApplicationService.class);
        TransferApplicationService transfers = transferContext.getBean(TransferApplicationService.class);
        NotificationWorkflowService notifications = notificationContext.getBean(NotificationWorkflowService.class);
        JdbcTemplate accountJdbc = accountContext.getBean(JdbcTemplate.class);
        JdbcTemplate transferJdbc = transferContext.getBean(JdbcTemplate.class);
        JdbcTemplate riskJdbc = riskContext.getBean(JdbcTemplate.class);

        var source = accounts.createAccount(new CreateAccountCommand("duplicate-source", "EUR"));
        var destination = accounts.createAccount(new CreateAccountCommand("duplicate-destination", "EUR"));
        accounts.addTestFunding(
                new FundAccountCommand(source.id(), new BigDecimal("1000.00"), "duplicate-initial-funding"));
        var transfer = transfers
                .create(new CreateTransferCommand(
                        source.id().value(),
                        destination.id().value(),
                        new Money(new BigDecimal("125.50"), SupportedCurrency.EUR),
                        new TransferReference("duplicate-replay"),
                        new IdempotencyKey("e2e-duplicate-key"),
                        new CorrelationId("e2e-duplicate-correlation")))
                .transfer();

        await().atMost(Duration.ofSeconds(60))
                .untilAsserted(
                        () -> assertThat(transfers.getTransfer(transfer.id()).status())
                                .isEqualTo(TransferStatus.COMPLETED));
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(
                                notifications.findByTransferId(transfer.id().value()))
                        .hasSize(1));

        String initiatedEnvelope =
                transferJdbc.queryForObject("""
                SELECT payload::text
                FROM outbox_events
                WHERE aggregate_id=? AND event_type='ledgerflow.transfer.initiated.v1'
                """, String.class, transfer.id().value());
        MeterRegistry meters = accountContext.getBean(MeterRegistry.class);
        double duplicatesBefore = meters.counter("kafka.consumer.duplicate", "service", "account-service")
                .count();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> publisher = transferContext.getBean(KafkaTemplate.class);
        publisher
                .send("ledgerflow.transfer.commands.v1", transfer.id().toString(), initiatedEnvelope)
                .join();

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(meters.counter("kafka.consumer.duplicate", "service", "account-service")
                                .count())
                        .isGreaterThan(duplicatesBefore));
        assertThat(accounts.getAccount(source.id()).availableBalance().formattedAmount())
                .isEqualTo("874.50");
        assertThat(accounts.getAccount(source.id()).reservedBalance().formattedAmount())
                .isEqualTo("0.00");
        assertThat(accounts.getAccount(destination.id()).availableBalance().formattedAmount())
                .isEqualTo("125.50");
        assertThat(accountJdbc.queryForObject(
                        "SELECT count(*) FROM ledger_entries WHERE reference LIKE ?",
                        Integer.class,
                        "transfer:" + transfer.id() + ":%"))
                .isEqualTo(2);
        assertThat(transferJdbc.queryForObject(
                        "SELECT count(*) FROM transfer_state_history WHERE transfer_id=?",
                        Integer.class,
                        transfer.id().value()))
                .isEqualTo(5);
        assertThat(riskJdbc.queryForObject(
                        "SELECT count(*) FROM risk_decisions WHERE transfer_id=?",
                        Integer.class,
                        transfer.id().value()))
                .isEqualTo(1);
        assertThat(notifications.findByTransferId(transfer.id().value())).hasSize(1);
    }

    @Test
    void riskRejectionRestoresReservationWithoutTransferLedgerEntries() {
        AccountApplicationService accounts = accountContext.getBean(AccountApplicationService.class);
        TransferApplicationService transfers = transferContext.getBean(TransferApplicationService.class);
        NotificationWorkflowService notifications = notificationContext.getBean(NotificationWorkflowService.class);

        var source = accounts.createAccount(new CreateAccountCommand("reject-source", "EUR"));
        var destination = accounts.createAccount(new CreateAccountCommand("reject-destination", "EUR"));
        accounts.addTestFunding(
                new FundAccountCommand(source.id(), new BigDecimal("1000.00"), "reject-initial-funding"));
        var transfer = transfers
                .create(new CreateTransferCommand(
                        source.id().value(),
                        destination.id().value(),
                        new Money(new BigDecimal("125.50"), SupportedCurrency.EUR),
                        new TransferReference("RISK-REJECT"),
                        new IdempotencyKey("e2e-rejected-key"),
                        new CorrelationId("e2e-rejected-correlation")))
                .transfer();

        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> assertThat(transfers.getTransfer(transfer.id()).status())
                                .isEqualTo(TransferStatus.REJECTED));

        var restored = accounts.getAccount(source.id());
        assertThat(restored.availableBalance().formattedAmount()).isEqualTo("1000.00");
        assertThat(restored.reservedBalance().formattedAmount()).isEqualTo("0.00");
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(
                                notifications.findByTransferId(transfer.id().value()))
                        .hasSize(1));
        assertThat(accountContext
                        .getBean(JdbcTemplate.class)
                        .queryForObject(
                                "SELECT count(*) FROM ledger_entries WHERE reference LIKE ?",
                                Integer.class,
                                "transfer:" + transfer.id() + ":%"))
                .isZero();
    }

    @Test
    void malformedCommandReachesDltWithOriginalRecordMetadata() {
        try (var consumer = dltConsumer("malformed-" + UUID.randomUUID())) {
            consumer.subscribe(java.util.List.of("ledgerflow.transfer.commands.dlt.v1"));
            consumer.poll(Duration.ofMillis(100));

            @SuppressWarnings("unchecked")
            KafkaTemplate<String, String> publisher = accountContext.getBean(KafkaTemplate.class);
            publisher
                    .send(
                            "ledgerflow.transfer.commands.v1",
                            UUID.randomUUID().toString(),
                            "{\"eventVersion\":2,\"payload\":{}}")
                    .join();

            await().atMost(Duration.ofSeconds(20))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        var records = consumer.poll(Duration.ofMillis(200));
                        assertThat(records).isNotEmpty();
                        var record = records.iterator().next();
                        assertThat(record.topic()).isEqualTo("ledgerflow.transfer.commands.dlt.v1");
                        assertThat(record.value()).contains("\"eventVersion\":2");
                        assertThat(Stream.of(record.headers().toArray())
                                        .map(header -> header.key().toLowerCase())
                                        .anyMatch(key -> key.contains("original-topic")))
                                .isTrue();
                    });
        }
    }

    @Test
    void insufficientFundsIsANormalBusinessRejectionAndDoesNotReachDlt() {
        try (var consumer = dltConsumer("business-rejection-" + UUID.randomUUID())) {
            var partitions = consumer.partitionsFor("ledgerflow.transfer.commands.dlt.v1").stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);

            AccountApplicationService accounts = accountContext.getBean(AccountApplicationService.class);
            TransferApplicationService transfers = transferContext.getBean(TransferApplicationService.class);
            var source = accounts.createAccount(new CreateAccountCommand("insufficient-source", "EUR"));
            var destination = accounts.createAccount(new CreateAccountCommand("insufficient-destination", "EUR"));
            accounts.addTestFunding(
                    new FundAccountCommand(source.id(), new BigDecimal("10.00"), "insufficient-funding"));
            var transfer = transfers
                    .create(new CreateTransferCommand(
                            source.id().value(),
                            destination.id().value(),
                            new Money(new BigDecimal("20.00"), SupportedCurrency.EUR),
                            new TransferReference("insufficient-business-outcome"),
                            new IdempotencyKey("e2e-insufficient-key"),
                            new CorrelationId("e2e-insufficient-correlation")))
                    .transfer();

            await().atMost(Duration.ofSeconds(60))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> assertThat(
                                    transfers.getTransfer(transfer.id()).status())
                            .isEqualTo(TransferStatus.REJECTED));

            assertThat(accounts.getAccount(source.id()).availableBalance().formattedAmount())
                    .isEqualTo("10.00");
            assertThat(accounts.getAccount(source.id()).reservedBalance().formattedAmount())
                    .isEqualTo("0.00");
            assertThat(accountContext
                            .getBean(JdbcTemplate.class)
                            .queryForObject(
                                    "SELECT count(*) FROM transfer_reservations WHERE transfer_id=?",
                                    Integer.class,
                                    transfer.id().value()))
                    .isZero();
            assertThat(consumer.poll(Duration.ofSeconds(1))).isEmpty();
        }
    }

    private void assertOutboxesPublished() {
        for (ConfigurableApplicationContext context : java.util.List.of(accountContext, transferContext, riskContext)) {
            await().atMost(Duration.ofSeconds(20))
                    .untilAsserted(() -> assertThat(context.getBean(JdbcTemplate.class)
                                    .queryForObject(
                                            "SELECT count(*) FROM outbox_events WHERE status <> 'PUBLISHED'",
                                            Integer.class))
                            .isZero());
        }
    }

    private static ConfigurableApplicationContext context(
            Class<?> application, Map<String, Object> database, Map<String, Object> extra) {
        java.util.HashMap<String, Object> properties = new java.util.HashMap<>(database);
        properties.putAll(extra);
        properties.put("spring.main.web-application-type", "none");
        properties.put("ledgerflow.outbox.poll-interval", "100ms");
        properties.put("ledgerflow.outbox.ack-timeout", "5s");
        properties.put("management.health.kafka.enabled", "false");
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(application)
                .web(WebApplicationType.NONE)
                .run(arguments);
    }

    private static Map<String, Object> database(PostgreSQLContainer database) {
        return Map.of(
                "spring.datasource.url", database.getJdbcUrl(),
                "spring.datasource.username", database.getUsername(),
                "spring.datasource.password", database.getPassword());
    }

    private static PostgreSQLContainer postgres(String name) {
        return new PostgreSQLContainer("postgres:18.4")
                .withDatabaseName("ledgerflow_" + name + "_e2e")
                .withUsername("ledgerflow_test")
                .withPassword("ledgerflow_test");
    }

    private KafkaConsumer<String, String> dltConsumer(String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }
}
