package dev.oguzkaandere.ledgerflow.transfer.adapter.in.kafka;

import dev.oguzkaandere.ledgerflow.transfer.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.transfer.application.service.TransferWorkflowService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class TransferWorkflowListener {
    private final ObjectMapper mapper;
    private final TransferWorkflowService workflow;

    TransferWorkflowListener(ObjectMapper mapper, TransferWorkflowService workflow) {
        this.mapper = mapper;
        this.workflow = workflow;
    }

    @KafkaListener(
            topics = "ledgerflow.account.events.v1",
            groupId = "${ledgerflow.kafka.groups.transfer-account:transfer-account-workflow-v1}",
            autoStartup = "${ledgerflow.kafka.listener-enabled:true}")
    void receiveAccountEvent(String json) {
        workflow.handle(mapper.readValue(json, WorkflowEnvelope.class));
    }

    @KafkaListener(
            topics = "ledgerflow.risk.events.v1",
            groupId = "${ledgerflow.kafka.groups.transfer-risk:transfer-risk-workflow-v1}",
            autoStartup = "${ledgerflow.kafka.listener-enabled:true}")
    void receiveRiskEvent(String json) {
        workflow.handle(mapper.readValue(json, WorkflowEnvelope.class));
    }
}
