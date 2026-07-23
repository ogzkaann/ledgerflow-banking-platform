package dev.oguzkaandere.ledgerflow.account.adapter.out.eventing;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class OutboxMetrics {
    OutboxMetrics(MeterRegistry registry, AccountEventStore store) {
        Gauge.builder("outbox.pending", store, AccountEventStore::pendingCount)
                .tag("service", "account-service")
                .register(registry);
        Gauge.builder("outbox.failed", store, AccountEventStore::failedCount)
                .tag("service", "account-service")
                .register(registry);
    }
}
