package dev.oguzkaandere.ledgerflow.risk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.risk.support.RiskIntegrationTest;
import org.junit.jupiter.api.Test;

class RiskServiceApplicationTests extends RiskIntegrationTest {
    @Test
    void contextLoadsWithFlywayAndJpaValidation() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(1);
    }
}
