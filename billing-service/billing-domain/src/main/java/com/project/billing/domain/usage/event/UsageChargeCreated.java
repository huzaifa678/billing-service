package com.project.billing.domain.usage.event;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.Metric;
import com.project.billing.domain.usage.UsageChargeId;

import java.time.Instant;
import java.util.Objects;

/**
 * Raised when a {@link com.project.billing.domain.usage.UsageCharge} is created.
 * Published to Kafka (topic {@code billing.usage-charge.created}) by the messaging
 * adapter after the surrounding transaction commits.
 */
public record UsageChargeCreated(
        UsageChargeId usageChargeId,
        InvoiceId invoiceId,
        Metric metric,
        long quantity,
        Money unitPrice,
        Money totalPrice,
        Instant occurredOn
) implements DomainEvent {

    public UsageChargeCreated {
        Objects.requireNonNull(usageChargeId, "usageChargeId");
        Objects.requireNonNull(invoiceId, "invoiceId");
        Objects.requireNonNull(metric, "metric");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(totalPrice, "totalPrice");
        Objects.requireNonNull(occurredOn, "occurredOn");
    }
}
