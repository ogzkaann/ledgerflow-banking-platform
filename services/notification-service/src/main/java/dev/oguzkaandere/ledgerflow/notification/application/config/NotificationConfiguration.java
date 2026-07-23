package dev.oguzkaandere.ledgerflow.notification.application.config;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationConfiguration {
    @Bean
    Clock notificationClock() {
        return Clock.systemUTC();
    }

    @Bean
    Supplier<UUID> notificationUuidGenerator() {
        return UUID::randomUUID;
    }
}
