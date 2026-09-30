package com.project.billing.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.project.billing.domain.shared.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Teaches Jackson to (de)serialize the {@link Money} value object, which has no default
 * constructor. Registered as a module bean so it applies to the ObjectMapper the event store and
 * snapshot store use for {@code jsonb} payloads — the domain stays free of Jackson annotations.
 */
@Configuration
public class MoneyJacksonModule {

    @Bean
    public SimpleModule moneyModule() {
        SimpleModule module = new SimpleModule("MoneyModule");
        module.addSerializer(Money.class, new MoneySerializer());
        module.addDeserializer(Money.class, new MoneyDeserializer());
        return module;
    }

    private static final class MoneySerializer extends JsonSerializer<Money> {
        @Override
        public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("currency", value.currency());
            gen.writeFieldName("amount");
            gen.writeNumber(value.amount());
            gen.writeEndObject();
        }
    }

    private static final class MoneyDeserializer extends JsonDeserializer<Money> {
        @Override
        public Money deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            BigDecimal amount = node.get("amount").decimalValue();
            String currency = node.get("currency").asText();
            return Money.of(amount, currency);
        }
    }
}
