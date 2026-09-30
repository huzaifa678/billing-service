package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import com.project.billing.domain.usage.Metric;
import com.project.billing.domain.usage.UsageChargeId;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import com.project.billing_service.avro.InvoiceCreated;
import com.project.billing_service.avro.InvoicePaid;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies domain events map to the right topic, partition key, and Avro payload. */
class BillingEventAvroMapperTest {

    private final BillingEventAvroMapper mapper = new BillingEventAvroMapper();

    @Test
    void mapsInvoiceCreatedToTopicKeyAndAvro() {
        InvoiceId id = InvoiceId.of(UUID.randomUUID());
        InvoiceEvent.Created event = InvoiceEvent.Created.of(
                id, SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID()),
                Money.of("29.99", "USD"), InvoiceStatus.ISSUED,
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(7));

        BillingEventAvroMapper.AvroMessage message = mapper.map(event);

        assertThat(message.topic()).isEqualTo("billing.invoice.created");
        assertThat(message.key()).isEqualTo(id.value().toString());
        assertThat(message.value()).isInstanceOf(InvoiceCreated.class);
        InvoiceCreated avro = (InvoiceCreated) message.value();
        assertThat(avro.getCurrency()).isEqualTo("USD");
        assertThat(avro.getAmount()).isEqualByComparingTo("29.9900"); // scaled to 4
        assertThat(avro.getStatus()).isEqualTo("ISSUED");
    }

    @Test
    void mapsInvoicePaid() {
        InvoiceId id = InvoiceId.of(UUID.randomUUID());
        BillingEventAvroMapper.AvroMessage message = mapper.map(InvoiceEvent.Paid.of(id));
        assertThat(message.topic()).isEqualTo("billing.invoice.paid");
        assertThat(message.value()).isInstanceOf(InvoicePaid.class);
    }

    @Test
    void mapsUsageChargeCreated() {
        UsageChargeCreated event = new UsageChargeCreated(
                UsageChargeId.of(UUID.randomUUID()), InvoiceId.of(UUID.randomUUID()),
                Metric.of("api_calls"), 2L, Money.of("10", "USD"), Money.of("20", "USD"), Instant.now());

        BillingEventAvroMapper.AvroMessage message = mapper.map(event);

        assertThat(message.topic()).isEqualTo("billing.usage-charge.created");
        assertThat(message.key()).isEqualTo(event.invoiceId().value().toString());
        assertThat(message.value()).isInstanceOf(com.project.billing_service.avro.UsageChargeCreated.class);
    }
}
