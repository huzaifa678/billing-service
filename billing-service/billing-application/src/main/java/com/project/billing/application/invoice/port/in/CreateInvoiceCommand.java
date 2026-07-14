package com.project.billing.application.invoice.port.in;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Command to create an invoice from explicit values (REST create flow). */
public record CreateInvoiceCommand(
        UUID subscriptionId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String status,
        OffsetDateTime issuedAt,
        OffsetDateTime dueAt
) {
}
