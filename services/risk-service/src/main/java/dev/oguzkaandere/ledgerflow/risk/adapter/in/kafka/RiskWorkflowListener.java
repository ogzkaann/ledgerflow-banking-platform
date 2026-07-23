package dev.oguzkaandere.ledgerflow.risk.adapter.in.kafka;

import dev.oguzkaandere.ledgerflow.risk.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.risk.application.service.RiskWorkflowService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class RiskWorkflowListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RiskWorkflowListener.class);
    private static final String FUNDS_RESERVED = "ledgerflow.account.funds-reserved.v1";
    private static final Set<String> KNOWN_UNCONSUMED_TYPES = Set.of(
            "ledgerflow.account.funds-reservation-rejected.v1",
            "ledgerflow.account.transfer-settled.v1",
            "ledgerflow.account.funds-released.v1");

    private final ObjectMapper mapper;
    private final RiskWorkflowService workflow;

    RiskWorkflowListener(ObjectMapper mapper, RiskWorkflowService workflow) {
        this.mapper = mapper;
        this.workflow = workflow;
    }

    @KafkaListener(
            topics = "ledgerflow.account.events.v1",
            groupId = "${ledgerflow.kafka.groups.risk:risk-account-workflow-v1}",
            autoStartup = "${ledgerflow.kafka.listener-enabled:true}")
    void receive(String json) {
        WorkflowEnvelope envelope = mapper.readValue(json, WorkflowEnvelope.class);
        KafkaMdc.run(envelope, () -> dispatch(envelope));
    }

    private void dispatch(WorkflowEnvelope envelope) {
        if (FUNDS_RESERVED.equals(envelope.eventType())) {
            workflow.handle(envelope);
        } else if (KNOWN_UNCONSUMED_TYPES.contains(envelope.eventType())) {
            LOGGER.debug(
                    "kafka_event_ignored service=risk-service eventId={} eventType={} correlationId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.correlationId());
        } else {
            throw new IllegalArgumentException("Unsupported Risk Service event type");
        }
    }
}
