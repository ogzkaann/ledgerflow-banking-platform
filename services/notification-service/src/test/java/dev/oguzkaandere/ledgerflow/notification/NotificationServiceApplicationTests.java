package dev.oguzkaandere.ledgerflow.notification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.notification.support.NotificationIntegrationTest;
import org.junit.jupiter.api.Test;

class NotificationServiceApplicationTests extends NotificationIntegrationTest {
    @Test
    void contextLoadsWithFlywayAndJpaValidation() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(1);
    }
}
