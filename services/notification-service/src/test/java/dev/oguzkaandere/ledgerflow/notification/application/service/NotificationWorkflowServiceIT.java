package dev.oguzkaandere.ledgerflow.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.oguzkaandere.ledgerflow.notification.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.notification.support.NotificationIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class NotificationWorkflowServiceIT extends NotificationIntegrationTest {
    @Autowired
    private NotificationWorkflowService workflow;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void completedEventCreatesOneDurableNotificationAndInspectionResponse() throws Exception {
        UUID transferId = UUID.randomUUID();
        WorkflowEnvelope event = terminal(NotificationWorkflowService.COMPLETED, transferId);
        workflow.handle(event);
        workflow.handle(event);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Integer.class))
                .isEqualTo(1);
        mockMvc.perform(get("/api/v1/notifications").queryParam("transferId", transferId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].finalTransferStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[0].messageTemplateKey").value("transfer-completed-v1"));
    }

    @Test
    void rejectedEventCreatesRejectionNotification() {
        workflow.handle(terminal(NotificationWorkflowService.REJECTED, UUID.randomUUID()));

        assertThat(jdbc.queryForObject("SELECT type FROM notifications", String.class))
                .isEqualTo("TRANSFER_REJECTED");
        assertThat(jdbc.queryForObject("SELECT final_transfer_status FROM notifications", String.class))
                .isEqualTo("REJECTED");
    }

    @Test
    void intermediateEventDoesNotCreateNotification() {
        assertThatThrownBy(() -> workflow.handle(terminal("ledgerflow.transfer.settling.v1", UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class))
                .isZero();
    }

    private WorkflowEnvelope terminal(String type, UUID transferId) {
        return new WorkflowEnvelope(
                UUID.randomUUID(),
                type,
                1,
                Instant.parse("2026-07-23T12:00:00Z"),
                "notification-correlation",
                "transfer-event",
                "transfer-service",
                mapper.valueToTree(Map.of("transferId", transferId)));
    }
}
