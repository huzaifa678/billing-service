package com.project.billing.adapter.in.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Consumer-side dead-letter wiring. When a {@code @KafkaListener}'s business logic
 * throws, the record is retried with exponential backoff; once the retries are
 * exhausted it is published to a {@code <topic>.DLT} dead-letter topic with the
 * failure captured in the record headers. Every failed attempt is logged so the
 * error is shipped to Loki via the OTLP appender.
 *
 * <p>Spring Boot's auto-configured {@code kafkaListenerContainerFactory} picks up
 * the single {@link DefaultErrorHandler} bean and applies it to every listener,
 * so the consumers need no further wiring.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final int MAX_RETRIES = 3;

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        // Route to <topic>.DLT, letting Kafka pick the partition (-1) so an
        // auto-created DLT with fewer partitions than the source still accepts the record.
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1)
        );
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(INITIAL_BACKOFF_MS, BACKOFF_MULTIPLIER);
        backOff.setMaxAttempts(MAX_RETRIES);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.error(
                        "Kafka consume failed (attempt {}/{}) topic={} partition={} offset={}; "
                                + "routing to {}.DLT once retries are exhausted",
                        deliveryAttempt, MAX_RETRIES, record.topic(), record.partition(),
                        record.offset(), record.topic(), ex
                ));
        return errorHandler;
    }
}
