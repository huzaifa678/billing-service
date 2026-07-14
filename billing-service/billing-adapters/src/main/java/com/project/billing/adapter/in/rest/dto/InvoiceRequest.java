package com.project.billing.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST request body for creating an invoice. Mirrors the previous {@code InvoiceDto}
 * wire contract; {@code invoiceId} is ignored on creation (identity is domain-assigned).
 */
public record InvoiceRequest(
        UUID invoiceId,
        UUID subscriptionId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String status,
        OffsetDateTime issuedAt,
        OffsetDateTime dueAt
) {
}
