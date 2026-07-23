package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.oguzkaandere.ledgerflow.transfer.support.TransferIntegrationTest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class TransferApiIT extends TransferIntegrationTest {
    private static final String REQUEST = """
            {
              "sourceAccountId":"0d17936c-05d5-45ae-9ee8-0a33f7ae8256",
              "destinationAccountId":"ae36fc23-a25d-40fb-a336-1a1604739880",
              "amount":"125.50",
              "currency":"EUR",
              "reference":"invoice-2026-001"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsRetrievesAndReplaysOneAtomicPendingTransfer() throws Exception {
        MvcResult created = create("request-001", REQUEST, "client-correlation", 202);
        String body = created.getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.transferId");

        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/v1/transfers/" + id);
        assertThat(created.getResponse().getHeader("X-Correlation-Id")).isEqualTo("client-correlation");
        assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("PENDING");
        assertThat(JsonPath.<String>read(body, "$.amount")).isEqualTo("125.50");

        mockMvc.perform(get("/api/v1/transfers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(id))
                .andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(get("/api/v1/transfers/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$[0].toStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].sequence").value(0));

        MvcResult replay = create("request-001", REQUEST.replace("125.50", "125.5"), "other-correlation", 202);
        assertThat(replay.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(JsonPath.<String>read(replay.getResponse().getContentAsString(), "$.transferId"))
                .isEqualTo(id);
        assertCounts(1, 1, 1, 1);
        assertThat(redis.getExpire("ledgerflow:idempotency:CREATE_TRANSFER:request-001"))
                .isBetween(23 * 60 * 60L, 24 * 60 * 60L);
    }

    @Test
    void rejectsDifferentPayloadAndUsesPostgresAfterCacheEviction() throws Exception {
        MvcResult first = create("request-002", REQUEST, null, 202);
        String id = JsonPath.read(first.getResponse().getContentAsString(), "$.transferId");
        assertThat(first.getResponse().getHeader("X-Correlation-Id")).isNotBlank();

        redis.delete("ledgerflow:idempotency:CREATE_TRANSFER:request-002");
        MvcResult restored = create("request-002", REQUEST, null, 202);
        assertThat(restored.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(JsonPath.<String>read(restored.getResponse().getContentAsString(), "$.transferId"))
                .isEqualTo(id);
        assertThat(redis.hasKey("ledgerflow:idempotency:CREATE_TRANSFER:request-002"))
                .isTrue();

        create("request-002", REQUEST.replace("125.50", "126.00"), null, 409);
        assertCounts(1, 1, 1, 1);
    }

    @Test
    void validatesHeadersBodyAccountsAndMissingResourcesAsProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request header"));

        create("bad-key spaces", REQUEST, null, 400);
        create(
                "same-account",
                REQUEST.replace("ae36fc23-a25d-40fb-a336-1a1604739880", "0d17936c-05d5-45ae-9ee8-0a33f7ae8256"),
                null,
                422);
        create("bad-money", REQUEST.replace("125.50", "0.001"), null, 422);
        create("bad-currency", REQUEST.replace("EUR", "CHF"), null, 422);

        mockMvc.perform(get("/api/v1/transfers/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Transfer not found"));
        mockMvc.perform(get("/api/v1/transfers/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void concurrentSameKeyProducesOneLogicalTransferAndOutboxEvent() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<MvcResult>> calls = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                calls.add(() -> create("concurrent-key", REQUEST, null, 202));
            }
            var futures = executor.invokeAll(calls);
            var ids = new HashSet<String>();
            for (var future : futures) {
                ids.add(JsonPath.read(future.get().getResponse().getContentAsString(), "$.transferId"));
            }
            assertThat(ids).hasSize(1);
        }
        assertCounts(1, 1, 1, 1);
    }

    @Test
    void concurrentDifferentPayloadProducesOneAcceptanceAndOneConflict() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<MvcResult> first = () -> createWithoutExpectation("racing-conflict", REQUEST);
            Callable<MvcResult> second =
                    () -> createWithoutExpectation("racing-conflict", REQUEST.replace("125.50", "126.00"));
            var futures = executor.invokeAll(List.of(first, second));
            assertThat(futures)
                    .extracting(future -> future.get().getResponse().getStatus())
                    .containsExactlyInAnyOrder(202, 409);
        }
        assertCounts(1, 1, 1, 1);
    }

    @Test
    void outboxFailureRollsBackAllFourWrites() throws Exception {
        jdbc.execute("""
                CREATE FUNCTION reject_test_outbox() RETURNS trigger AS $$
                BEGIN
                    IF NEW.payload #>> '{payload,reference}' = 'rollback-test' THEN
                        RAISE EXCEPTION 'controlled outbox failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbc.execute("""
                CREATE TRIGGER reject_test_outbox_trigger
                BEFORE INSERT ON outbox_events
                FOR EACH ROW EXECUTE FUNCTION reject_test_outbox()
                """);
        try {
            create("rollback-key", REQUEST.replace("invoice-2026-001", "rollback-test"), null, 500);
            assertCounts(0, 0, 0, 0);
        } finally {
            jdbc.execute("DROP TRIGGER reject_test_outbox_trigger ON outbox_events");
            jdbc.execute("DROP FUNCTION reject_test_outbox()");
        }
    }

    @Test
    void flywaySchemaValidationAndOutboxEnvelopeAreRealPostgresJsonb() throws Exception {
        create("schema-key", REQUEST, null, 202);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT payload ->> 'eventType' FROM outbox_events", String.class))
                .isEqualTo("ledgerflow.transfer.initiated.v1");
        assertThat(jdbc.queryForObject("SELECT payload #>> '{payload,amount}' FROM outbox_events", String.class))
                .isEqualTo("125.50");
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events", String.class))
                .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT published_at IS NULL FROM outbox_events", Boolean.class))
                .isTrue();
    }

    private MvcResult create(String key, String body, String correlation, int expectedStatus) throws Exception {
        var builder = post("/api/v1/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (correlation != null) {
            builder.header("X-Correlation-Id", correlation);
        }
        return mockMvc.perform(builder).andExpect(status().is(expectedStatus)).andReturn();
    }

    private MvcResult createWithoutExpectation(String key, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private void assertCounts(int transfers, int history, int idempotency, int outbox) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transfers", Integer.class))
                .isEqualTo(transfers);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transfer_state_history", Integer.class))
                .isEqualTo(history);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_records", Integer.class))
                .isEqualTo(idempotency);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
                .isEqualTo(outbox);
    }
}
