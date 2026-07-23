package dev.oguzkaandere.ledgerflow.notification.adapter.in.kafka;

import dev.oguzkaandere.ledgerflow.notification.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.notification.application.service.NotificationWorkflowService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class NotificationWorkflowListener {
    private final ObjectMapper mapper;
    private final NotificationWorkflowService workflow;

    NotificationWorkflowListener(ObjectMapper mapper, NotificationWorkflowService workflow) {
        this.mapper = mapper;
        this.workflow = workflow;
    }

    @KafkaListener(
            topics = "ledgerflow.transfer.events.v1",
            groupId = "${ledgerflow.kafka.groups.notification:notification-transfer-workflow-v1}",
            autoStartup = "${ledgerflow.kafka.listener-enabled:true}")
    void receive(String json) {
        workflow.handle(mapper.readValue(json, WorkflowEnvelope.class));
    }
}
