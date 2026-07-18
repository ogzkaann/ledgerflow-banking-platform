package dev.oguzkaandere.ledgerflow.account;

import static org.assertj.core.api.Assertions.assertThat;

import dev.oguzkaandere.ledgerflow.account.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;

class AccountServiceApplicationIT extends PostgresIntegrationTest {

    @Test
    void contextLoadsWithFlywayManagedSchemaAndValidatedJpaMappings() {
        Integer migrationCount =
                jdbcTemplate.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);

        assertThat(migrationCount).isEqualTo(1);
    }
}
