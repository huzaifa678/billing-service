package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for reading invoices from the read-model projection. The query side depends on
 * this rather than on the event-sourced write repository, so reads are served from the
 * materialised {@code invoices} table without replaying events.
 */
public interface InvoiceReadModelPort {

    Optional<Invoice> findById(InvoiceId id);

    List<Invoice> findByCustomerId(CustomerId customerId);

    List<Invoice> findByStatus(InvoiceStatus status);

    Optional<Invoice> findBySubscriptionId(SubscriptionId subscriptionId);

    /**
     * Load the projection row under a {@code SELECT ... FOR UPDATE} lock held until the current
     * transaction commits. The event-sourced repository uses this to serialize concurrent payment
     * attempts on the same invoice (the lock is on the always-present projection row), preserving
     * the double-charge protection that pessimistic locking gave before event sourcing.
     */
    Optional<Invoice> findByIdForUpdate(InvoiceId id);
}
