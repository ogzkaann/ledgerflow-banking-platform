package dev.oguzkaandere.ledgerflow.account.application.config;

import dev.oguzkaandere.ledgerflow.account.domain.service.AccountReconciler;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AccountServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Supplier<UUID> uuidGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    AccountReconciler accountReconciler() {
        return new AccountReconciler();
    }
}
