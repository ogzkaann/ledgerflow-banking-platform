package dev.oguzkaandere.ledgerflow.risk.adapter.out.eventing;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class OutboxMetrics {
    OutboxMetrics(MeterRegistry registry, RiskEventStore store) {
        Gauge.builder("outbox.pending", store, RiskEventStore::pendingCount)
                .tag("service", "risk-service")
                .register(registry);
        Gauge.builder("outbox.failed", store, RiskEventStore::failedCount)
                .tag("service", "risk-service")
                .register(registry);
    }
}
