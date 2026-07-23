package dev.oguzkaandere.ledgerflow.notification.adapter.in.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
class KafkaFailureConfiguration {
    @Bean
    DefaultErrorHandler notificationKafkaErrorHandler(KafkaTemplate<String, String> kafka, MeterRegistry meters) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka, (record, exception) -> {
            meters.counter("kafka.dlt.publications", "service", "notification-service")
                    .increment();
            return new TopicPartition(dlt(record.topic()), record.partition());
        });
        recoverer.setFailIfSendResultIsError(true);
        var backoff = new ExponentialBackOffWithMaxRetries(2);
        backoff.setInitialInterval(1000);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(4000);
        var handler = new DefaultErrorHandler(recoverer, backoff);
        handler.setRetryListeners((record, exception, deliveryAttempt) -> meters.counter(
                        "kafka.listener.failures", "service", "notification-service")
                .increment());
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setCommitRecovered(true);
        return handler;
    }

    private static String dlt(String topic) {
        return topic.replace(".v1", ".dlt.v1");
    }
}
