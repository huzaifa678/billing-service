package com.project.billing.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Jackson 2 {@link ObjectMapper} for the event store / snapshot store jsonb payloads.
 *
 * <p>Spring Boot 4 autoconfigures Jackson 3 ({@code tools.jackson}) and no longer exposes a
 * {@code com.fasterxml.jackson.databind.ObjectMapper} bean, but {@link BillingEventSerializer}
 * and {@code InvoiceSnapshotStoreAdapter} are built on Jackson 2. This defines that mapper
 * explicitly, registering every Jackson 2 {@link Module} bean (so {@code MoneyJacksonModule}'s
 * Money (de)serializer applies) plus JSR-310, and disabling
 * {@code ADJUST_DATES_TO_CONTEXT_TIME_ZONE} so event replay reproduces the recorded offset
 * rather than normalizing it to UTC.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(List<Module> modules) {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModules(modules)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .build();
    }
}
