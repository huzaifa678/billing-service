package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.Invoice;

/**
 * Outbound port for the invoice read-model projection. The event-sourced repository calls this in
 * the same transaction as the event append, so the {@code invoices} table the query side reads
 * stays consistent with the event store.
 */
public interface InvoiceProjectionPort {

    /** Upsert the read-model row to reflect the aggregate's current state. */
    void project(Invoice invoice);
}
