package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import com.project.billing_service.avro.InvoiceActivated;
import com.project.billing_service.avro.InvoiceCreated;
import com.project.billing_service.avro.InvoiceFailed;
import com.project.billing_service.avro.InvoicePaid;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Maps stored billing {@link DomainEvent}s to the Avro records published on Kafka, so the outbox
 * relay's transport format matches the platform's Schema-Registry standard. Each event maps to a
 * topic, a partition key, and an Avro payload.
 *
 * <p>Dispatch is a per-type registry rather than a {@code switch}: each event type registers its own
 * focused builder method, so adding an event is a one-line registration plus a small method, and the
 * hot path is a map lookup. (A registry fits here because {@code map} dispatches over the open
 * {@link DomainEvent} type — spanning the invoice events and {@code UsageChargeCreated} — where a
 * switch would need a default anyway.)
 */
@Component
public class BillingEventAvroMapper {

    private static final int INVOICE_MONEY_SCALE = 4;
    private static final int USAGE_MONEY_SCALE = 2;

    /** A ready-to-send Kafka message: which topic, which key, and the Avro value. */
    public record AvroMessage(String topic, String key, Object value) {
    }

    private final Map<Class<? extends DomainEvent>, Function<DomainEvent, AvroMessage>> mappers = new HashMap<>();

    public BillingEventAvroMapper() {
        register(InvoiceEvent.Created.class, this::invoiceCreated);
        register(InvoiceEvent.Activated.class, this::invoiceActivated);
        register(InvoiceEvent.Paid.class, this::invoicePaid);
        register(InvoiceEvent.Failed.class, this::invoiceFailed);
        register(UsageChargeCreated.class, this::usageChargeCreated);
    }

    public AvroMessage map(DomainEvent event) {
        Function<DomainEvent, AvroMessage> mapper = mappers.get(event.getClass());
        if (mapper == null) {
            throw new IllegalArgumentException("No Avro mapping for billing event " + event.getClass().getName());
        }
        return mapper.apply(event);
    }

    /** Register a typed builder, wrapping it so the registry can dispatch on the erased event type. */
    private <E extends DomainEvent> void register(Class<E> type, Function<E, AvroMessage> mapper) {
        mappers.put(type, event -> mapper.apply(type.cast(event)));
    }

    private AvroMessage invoiceCreated(InvoiceEvent.Created e) {
        return new AvroMessage("billing.invoice.created", key(e.invoiceId().value()),
                InvoiceCreated.newBuilder()
                        .setInvoiceId(e.invoiceId().value())
                        .setSubscriptionId(e.subscriptionId().value())
                        .setCustomerId(e.customerId().value())
                        .setAmount(e.amount().amount().setScale(INVOICE_MONEY_SCALE, RoundingMode.HALF_UP))
                        .setCurrency(e.amount().currency())
                        .setStatus(e.status().name())
                        .setIssuedAt(e.issuedAt().toInstant())
                        .setDueAt(e.dueAt().toInstant())
                        .setOccurredOn(e.occurredOn())
                        .build());
    }

    private AvroMessage invoiceActivated(InvoiceEvent.Activated e) {
        return new AvroMessage("billing.invoice.activated", key(e.invoiceId().value()),
                InvoiceActivated.newBuilder()
                        .setInvoiceId(e.invoiceId().value())
                        .setOccurredOn(e.occurredOn())
                        .build());
    }

    private AvroMessage invoicePaid(InvoiceEvent.Paid e) {
        return new AvroMessage("billing.invoice.paid", key(e.invoiceId().value()),
                InvoicePaid.newBuilder()
                        .setInvoiceId(e.invoiceId().value())
                        .setOccurredOn(e.occurredOn())
                        .build());
    }

    private AvroMessage invoiceFailed(InvoiceEvent.Failed e) {
        return new AvroMessage("billing.invoice.failed", key(e.invoiceId().value()),
                InvoiceFailed.newBuilder()
                        .setInvoiceId(e.invoiceId().value())
                        .setOccurredOn(e.occurredOn())
                        .build());
    }

    private AvroMessage usageChargeCreated(UsageChargeCreated e) {
        return new AvroMessage("billing.usage-charge.created", key(e.invoiceId().value()),
                com.project.billing_service.avro.UsageChargeCreated.newBuilder()
                        .setUsageChargeId(e.usageChargeId().value())
                        .setInvoiceId(e.invoiceId().value())
                        .setMetric(e.metric().value())
                        .setQuantity(e.quantity())
                        .setUnitPrice(e.unitPrice().amount().setScale(USAGE_MONEY_SCALE, RoundingMode.HALF_UP))
                        .setTotalPrice(e.totalPrice().amount().setScale(USAGE_MONEY_SCALE, RoundingMode.HALF_UP))
                        .setCreatedAt(e.occurredOn())
                        .build());
    }

    private static String key(UUID id) {
        return id.toString();
    }
}
