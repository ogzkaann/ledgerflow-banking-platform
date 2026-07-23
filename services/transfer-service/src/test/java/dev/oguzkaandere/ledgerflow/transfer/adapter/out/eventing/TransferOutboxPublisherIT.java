package dev.oguzkaandere.ledgerflow.transfer.adapter.out.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.transfer.TransferServiceApplication;
import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.application.service.TransferApplicationService;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import dev.oguzkaandere.ledgerflow.transfer.support.TransferIntegrationTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

class TransferOutboxPublisherIT extends TransferIntegrationTest {
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.1.2");

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private TransferApplicationService transfers;

    @Autowired
    private TransferOutboxPublisher publisher;

    @Test
    void initiatedOutboxPublishesWithTransferKeyOnlyAfterBrokerAcknowledgement() {
        var transfer = transfers
                .create(new CreateTransferCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new Money(new BigDecimal("125.50"), SupportedCurrency.EUR),
                        new TransferReference("kafka-outbox"),
                        new IdempotencyKey("kafka-outbox-key"),
                        new CorrelationId("kafka-correlation")))
                .transfer();

        publisher.publishPending();

        var consumerProperties =
                KafkaTestUtils.consumerProps(KAFKA.getBootstrapServers(), "transfer-outbox-test", "true");
        var consumer = new DefaultKafkaConsumerFactory<>(
                        consumerProperties, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        try {
            consumer.subscribe(java.util.List.of("ledgerflow.transfer.commands.v1"));
            var record = recordFor(consumer, transfer.id().value());
            assertThat(record.key()).isEqualTo(transfer.id().toString());
            assertThat(record.value()).contains("ledgerflow.transfer.initiated.v1");
        } finally {
            consumer.close();
        }
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events", String.class))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("SELECT published_at IS NOT NULL FROM outbox_events", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT publish_attempt_count FROM outbox_events", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void freshApplicationContextRecoversDurablePendingOutboxAfterRestart() {
        var transfer = transfers
                .create(new CreateTransferCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new Money(new BigDecimal("50.00"), SupportedCurrency.EUR),
                        new TransferReference("restart-recovery"),
                        new IdempotencyKey("restart-recovery-key"),
                        new CorrelationId("restart-recovery-correlation")))
                .transfer();
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events", String.class))
                .isEqualTo("PENDING");

        try (var recoveryContext = new SpringApplicationBuilder(TransferServiceApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--spring.data.redis.host=" + REDIS.getHost(),
                        "--spring.data.redis.port=" + REDIS.getMappedPort(6379),
                        "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                        "--ledgerflow.kafka.listener-enabled=false",
                        "--ledgerflow.outbox.scheduling-enabled=false",
                        "--management.health.kafka.enabled=false")) {
            recoveryContext.getBean(TransferOutboxPublisher.class).publishPending();
        }

        var consumerProperties =
                KafkaTestUtils.consumerProps(KAFKA.getBootstrapServers(), "transfer-restart-test", "true");
        var consumer = new DefaultKafkaConsumerFactory<>(
                        consumerProperties, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        try {
            consumer.subscribe(java.util.List.of("ledgerflow.transfer.commands.v1"));
            var record = recordFor(consumer, transfer.id().value());
            assertThat(record.key()).isEqualTo(transfer.id().toString());
        } finally {
            consumer.close();
        }
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events", String.class))
                .isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("SELECT publish_attempt_count FROM outbox_events", Integer.class))
                .isEqualTo(1);
    }

    private static ConsumerRecord<String, String> recordFor(Consumer<String, String> consumer, UUID transferId) {
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
        return StreamSupport.stream(records.spliterator(), false)
                .filter(record -> transferId.toString().equals(record.key()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Kafka record found for transfer " + transferId));
    }
}
