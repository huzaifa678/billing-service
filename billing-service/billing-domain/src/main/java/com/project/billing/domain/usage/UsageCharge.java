package com.project.billing.domain.usage;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.AbstractAggregateRoot;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.event.UsageChargeCreated;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UsageCharge aggregate root. Computes its own total price (unit price × quantity) and raises
 * {@link UsageChargeCreated} on creation. It is a create-only aggregate — its event stream is a
 * single {@code Created} fact — so it is event-sourced for a uniform write path (event store +
 * outbox relay) without needing snapshots.
 */
public class UsageCharge extends AbstractAggregateRoot {

    private final UsageChargeId id;
    private final InvoiceId invoiceId;
    private final Metric metric;
    private final long quantity;
    private final Money unitPrice;
    private final Money totalPrice;

    private UsageCharge(
            UsageChargeId id,
            InvoiceId invoiceId,
            Metric metric,
            long quantity,
            Money unitPrice,
            Money totalPrice
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
        this.metric = Objects.requireNonNull(metric, "metric");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice");
    }

    /** Create a new usage charge; total price is derived as unitPrice × quantity. */
    public static UsageCharge create(
            InvoiceId invoiceId,
            Metric metric,
            long quantity,
            Money unitPrice
    ) {
        Money totalPrice = unitPrice.multiply(quantity);
        UsageCharge charge = new UsageCharge(
                UsageChargeId.of(UUID.randomUUID()), invoiceId, metric, quantity, unitPrice, totalPrice
        );
        charge.registerEvent(new UsageChargeCreated(
                charge.id, invoiceId, metric, quantity, unitPrice, totalPrice, Instant.now()
        ));
        charge.markApplied();
        return charge;
    }

    /** Rebuild a usage charge from its (single-event) stream. */
    public static UsageCharge replay(List<DomainEvent> events) {
        if (events == null || events.size() != 1 || !(events.get(0) instanceof UsageChargeCreated created)) {
            throw new IllegalArgumentException("A usage charge is rebuilt from exactly one Created event.");
        }
        UsageCharge charge = new UsageCharge(
                created.usageChargeId(),
                created.invoiceId(),
                created.metric(),
                created.quantity(),
                created.unitPrice(),
                created.totalPrice()
        );
        charge.markApplied();
        return charge;
    }

    public UsageChargeId id() {
        return id;
    }

    public InvoiceId invoiceId() {
        return invoiceId;
    }

    public Metric metric() {
        return metric;
    }

    public long quantity() {
        return quantity;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Money totalPrice() {
        return totalPrice;
    }
}
