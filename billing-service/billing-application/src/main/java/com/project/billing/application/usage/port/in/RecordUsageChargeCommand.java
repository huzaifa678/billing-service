package com.project.billing.application.usage.port.in;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.Metric;

/** Command to record a usage charge against an invoice. */
public record RecordUsageChargeCommand(
        InvoiceId invoiceId,
        Metric metric,
        long quantity,
        Money unitPrice
) {
}
