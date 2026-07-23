package dev.oguzkaandere.ledgerflow.account.adapter.out.eventing;

import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component("kafkaHealthIndicator")
class KafkaHealthIndicator implements HealthIndicator {
    private final KafkaAdmin kafkaAdmin;

    KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            String clusterId = admin.describeCluster().clusterId().get(2, TimeUnit.SECONDS);
            return Health.up().withDetail("clusterId", clusterId).build();
        } catch (Exception exception) {
            return Health.down()
                    .withDetail("exceptionType", exception.getClass().getSimpleName())
                    .build();
        }
    }
}
