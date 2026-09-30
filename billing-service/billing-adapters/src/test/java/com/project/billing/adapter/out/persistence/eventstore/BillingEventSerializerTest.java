package com.project.billing.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import com.project.billing.domain.usage.Metric;
import com.project.billing.domain.usage.UsageChargeId;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trips every stored billing event through the serializer with a Jackson mapper configured
 * like the application's (Java-time as ISO strings so offsets survive, record parameter names, and
 * the {@link MoneyJacksonModule}), verifying the JSON contract without a database.
 */
class BillingEventSerializerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Keep the stored offset on replay instead of normalising to UTC, so events round-trip
            // exactly (this mirrors the application's Jackson config).
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .registerModule(new MoneyJacksonModule().moneyModule());

    private final BillingEventSerializer serializer = new BillingEventSerializer(mapper);

    private void assertRoundTrips(DomainEvent event) {
        String type = serializer.typeOf(event);
        DomainEvent restored = serializer.deserialize(type, serializer.serialize(event));
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void invoiceEventsRoundTrip() {
        InvoiceId id = InvoiceId.of(UUID.randomUUID());
        assertRoundTrips(InvoiceEvent.Created.of(
                id, SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID()),
                Money.of("29.99", "USD"), InvoiceStatus.ISSUED,
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(7)));
        assertRoundTrips(InvoiceEvent.Activated.of(id));
        assertRoundTrips(InvoiceEvent.Paid.of(id));
        assertRoundTrips(InvoiceEvent.Failed.of(id));
    }

    @Test
    void usageChargeCreatedRoundTrips() {
        assertRoundTrips(new UsageChargeCreated(
                UsageChargeId.of(UUID.randomUUID()), InvoiceId.of(UUID.randomUUID()),
                Metric.of("api_calls"), 5L, Money.of("1.50", "USD"), Money.of("7.50", "USD"), Instant.now()));
    }

    @Test
    void typeTagsAreStable() {
        InvoiceId id = InvoiceId.of(UUID.randomUUID());
        assertThat(serializer.typeOf(InvoiceEvent.Paid.of(id))).isEqualTo("invoice.paid");
    }

    @Test
    void unknownTypeIsRejected() {
        assertThatThrownBy(() -> serializer.deserialize("invoice.unknown", "{}"))
                .isInstanceOf(IllegalStateException.class);
    }
}
