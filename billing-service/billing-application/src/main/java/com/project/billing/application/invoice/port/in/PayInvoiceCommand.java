package com.project.billing.application.invoice.port.in;

import java.util.UUID;

/** Command to pay an invoice with a given payment method. */
public record PayInvoiceCommand(UUID invoiceId, String paymentMethodId) {
}
