package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;

import java.util.Optional;

/**
 * Outbound port for invoice snapshots — a materialised aggregate state at a known version.
 * Loading a snapshot and replaying only the events after it bounds replay cost. Snapshots are a
 * pure optimisation and can be dropped and rebuilt from the event store at any time.
 */
public interface InvoiceSnapshotStorePort {

    Optional<Invoice> load(InvoiceId id);

    void save(Invoice invoice);
}
