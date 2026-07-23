package dev.oguzkaandere.ledgerflow.account.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.oguzkaandere.ledgerflow.account.support.PostgresIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class AccountApiIT extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndRetrievesAccountWithoutExposingPersistenceTypes() throws Exception {
        String accountId = createAccount("web-customer", "EUR");

        mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.ownerReference").value("web-customer"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availableBalance").value("0.00"))
                .andExpect(jsonPath("$.reservedBalance").value("0.00"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void validationAndUnsupportedCurrencyUseDocumentedProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerReference\":\"   \",\"currency\":\"EUR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("ownerReference"));

        mockMvc.perform(post("/api/v1/accounts")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerReference\":\"customer-chf\",\"currency\":\"CHF\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Unsupported currency"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void missingAndMalformedAccountIdentifiersAreSafeProblemDetails() throws Exception {
        UUID missing = UUID.fromString("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(get("/api/v1/accounts/{accountId}", missing).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Account not found"))
                .andExpect(jsonPath("$.detail").value("Account " + missing + " does not exist"))
                .andExpect(jsonPath("$.instance").value("/api/v1/accounts/" + missing));

        mockMvc.perform(get("/api/v1/accounts/not-a-uuid").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request parameter"));
    }

    @Test
    void localProfileFundingIsAtomicAndDuplicateReferenceReturnsConflict() throws Exception {
        String accountId = createAccount("funding-customer", "GBP");
        String funding = "{\"amount\":\"1000.00\",\"reference\":\"initial-funding-001\"}";

        mockMvc.perform(post("/api/v1/accounts/{accountId}/test-funding", accountId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funding))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.account.availableBalance").value("1000.00"))
                .andExpect(jsonPath("$.account.currency").value("GBP"))
                .andExpect(jsonPath("$.ledgerEntry.type").value("CREDIT"))
                .andExpect(jsonPath("$.ledgerEntry.currency").value("GBP"))
                .andExpect(jsonPath("$.ledgerEntry.reference").value("initial-funding-001"));

        mockMvc.perform(post("/api/v1/accounts/{accountId}/test-funding", accountId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funding))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate funding reference"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void ledgerEndpointReturnsStablePaginationMetadataAndRejectsOversizedPages() throws Exception {
        String accountId = createAccount("ledger-customer", "USD");
        addFunding(accountId, "10.00", "funding-1");
        addFunding(accountId, "20.00", "funding-2");
        jdbcTemplate.update(
                "UPDATE ledger_entries SET created_at = ? WHERE reference = ?",
                Timestamp.from(Instant.parse("2026-07-18T12:00:01Z")),
                "funding-1");
        jdbcTemplate.update(
                "UPDATE ledger_entries SET created_at = ? WHERE reference = ?",
                Timestamp.from(Instant.parse("2026-07-18T12:00:02Z")),
                "funding-2");

        mockMvc.perform(get("/api/v1/accounts/{accountId}/ledger", accountId)
                        .with(admin())
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].reference").value("funding-2"));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/ledger", accountId)
                        .with(admin())
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Request validation failed"));
    }

    private String createAccount(String ownerReference, String currency) throws Exception {
        String response = mockMvc.perform(post("/api/v1/accounts")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerReference":"%s","currency":"%s"}
                                """.formatted(ownerReference, currency)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/accounts/")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(response).read("$.accountId");
    }

    private void addFunding(String accountId, String amount, String reference) throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{accountId}/test-funding", accountId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"%s","reference":"%s"}
                                """.formatted(amount, reference)))
                .andExpect(status().isCreated());
    }

    @Test
    void enforcesDirectServiceRolesAndAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ledgerflow-auditor")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerReference\":\"audit-write\",\"currency\":\"EUR\"}"))
                .andExpect(status().isForbidden());

        String accountId = createAccount("security-funding", "EUR");
        mockMvc.perform(post("/api/v1/accounts/{accountId}/test-funding", accountId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ledgerflow-operator")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"10.00\",\"reference\":\"forbidden-funding\"}"))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ledgerflow-admin"));
    }
}
