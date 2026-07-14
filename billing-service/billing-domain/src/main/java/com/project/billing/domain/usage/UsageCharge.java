package com.project.billing.domain.usage;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.AbstractAggregateRoot;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.event.UsageChargeCreated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * UsageCharge aggregate root. Computes its own total price from unit price × quantity
 * and raises {@link UsageChargeCreated} on creation, replacing the arithmetic that
 * previously lived in {@code UsageChargeService}.
 */
public class UsageCharge extends AbstractAggregateRoot {

    private final UsageChargeId id;
    private final InvoiceId invoiceId;
    private final Metric metric;
    private final long quantity;
    private final Money unitPrice;
    private final Money totalPrice;
    private final transient boolean isNew;

    private UsageCharge(
            UsageChargeId id,
            InvoiceId invoiceId,
            Metric metric,
            long quantity,
            Money unitPrice,
            Money totalPrice,
            boolean isNew
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
        this.isNew = isNew;
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
                UsageChargeId.of(UUID.randomUUID()),
                invoiceId, metric, quantity, unitPrice, totalPrice, true
        );
        charge.registerEvent(new UsageChargeCreated(
                charge.id, invoiceId, metric, quantity, unitPrice, totalPrice, Instant.now()
        ));
        return charge;
    }

    public boolean isNew() {
        return isNew;
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
