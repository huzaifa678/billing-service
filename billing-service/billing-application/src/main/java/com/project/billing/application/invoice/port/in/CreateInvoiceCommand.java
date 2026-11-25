package com.project.billing.application.invoice.port.in;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Command to create an invoice from explicit values (REST create flow).
 *
 * <p>{@code idempotencyKey} is optional (the forwarded {@code Idempotency-Key} header): when
 * present it makes the invoice id deterministic, so a retried create returns the original invoice
 * instead of raising a second one.
 */
public record CreateInvoiceCommand(
        UUID subscriptionId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String status,
        OffsetDateTime issuedAt,
        OffsetDateTime dueAt,
        String idempotencyKey
) {
}
