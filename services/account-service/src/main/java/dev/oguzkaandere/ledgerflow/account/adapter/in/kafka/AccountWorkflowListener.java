package dev.oguzkaandere.ledgerflow.account.adapter.in.kafka;

import dev.oguzkaandere.ledgerflow.account.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.account.application.service.AccountTransferWorkflowService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class AccountWorkflowListener {
    private final ObjectMapper mapper;
    private final AccountTransferWorkflowService workflow;

    AccountWorkflowListener(ObjectMapper mapper, AccountTransferWorkflowService workflow) {
        this.mapper = mapper;
        this.workflow = workflow;
    }

    @KafkaListener(
            topics = "ledgerflow.transfer.commands.v1",
            groupId = "${ledgerflow.kafka.groups.account:account-transfer-workflow-v1}",
            autoStartup = "${ledgerflow.kafka.listener-enabled:true}")
    void receive(String json) {
        workflow.handle(mapper.readValue(json, WorkflowEnvelope.class));
    }
}
