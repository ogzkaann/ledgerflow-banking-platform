package dev.oguzkaandere.ledgerflow.transfer.adapter.out.eventing;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class OutboxMetrics {
    OutboxMetrics(MeterRegistry registry, TransferOutboxPublisher publisher) {
        Gauge.builder("outbox.pending", publisher, TransferOutboxPublisher::pendingCount)
                .tag("service", "transfer-service")
                .register(registry);
        Gauge.builder("outbox.failed", publisher, TransferOutboxPublisher::failedCount)
                .tag("service", "transfer-service")
                .register(registry);
    }
}
