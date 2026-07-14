package com.project.billing.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** REST response body for an invoice. Field names match the previous entity JSON. */
public record InvoiceResponse(
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
