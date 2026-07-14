package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;

import java.util.List;
import java.util.Optional;

/** Outbound port for persisting and loading {@link Invoice} aggregates. */
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    Optional<Invoice> findBySubscriptionId(SubscriptionId subscriptionId);

    List<Invoice> findByCustomerId(CustomerId customerId);

    List<Invoice> findByStatus(InvoiceStatus status);
}
