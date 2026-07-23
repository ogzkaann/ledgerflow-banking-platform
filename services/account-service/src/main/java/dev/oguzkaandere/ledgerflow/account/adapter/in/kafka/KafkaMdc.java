package dev.oguzkaandere.ledgerflow.account.adapter.in.kafka;

import dev.oguzkaandere.ledgerflow.account.application.event.WorkflowEnvelope;
import java.util.Map;
import org.slf4j.MDC;

final class KafkaMdc {
    private KafkaMdc() {}

    static void run(WorkflowEnvelope envelope, Runnable action) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.put("correlationId", envelope.correlationId());
            MDC.put("eventId", envelope.eventId().toString());
            MDC.put("eventType", envelope.eventType());
            MDC.put("causationId", envelope.causationId());
            var transferId = envelope.payload().get("transferId");
            if (transferId != null && transferId.isTextual()) {
                MDC.put("transferId", transferId.asText());
            }
            action.run();
        } finally {
            MDC.clear();
            if (previous != null) {
                MDC.setContextMap(previous);
            }
        }
    }
}
