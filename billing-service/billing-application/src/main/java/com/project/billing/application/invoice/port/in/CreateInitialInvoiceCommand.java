package com.project.billing.application.invoice.port.in;

import java.util.UUID;

/** Command to create the initial invoice when a subscription is created. */
public record CreateInitialInvoiceCommand(UUID subscriptionId, UUID customerId) {
}
