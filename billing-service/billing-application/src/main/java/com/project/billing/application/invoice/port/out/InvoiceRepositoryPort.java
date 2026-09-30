package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.SubscriptionId;

import java.util.Optional;

/**
 * Command-side (write) outbound port for {@link Invoice} aggregates, backed by the event store.
 * Reads for the query side go through {@link InvoiceReadModelPort} instead; the loads here return
 * event-sourced aggregates (with a version) because they feed load-mutate-save command flows.
 */
public interface InvoiceRepositoryPort {

    /** Append the aggregate's pending events, project the read model, and snapshot as needed. */
    Invoice save(Invoice invoice);

    /**
     * Load an invoice for a payment attempt under a pessimistic lock on its projection row (so
     * concurrent pays on the same invoice are serialized), rebuilt from the event store.
     * Must be called within a transaction.
     */
    Optional<Invoice> findByIdForUpdate(InvoiceId id);

    /** Load the invoice for a subscription (subscription-driven activation/failure flows). */
    Optional<Invoice> findBySubscriptionId(SubscriptionId subscriptionId);
}
