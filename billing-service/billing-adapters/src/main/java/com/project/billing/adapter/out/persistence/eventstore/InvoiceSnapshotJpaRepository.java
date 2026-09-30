package com.project.billing.adapter.out.persistence.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data repository over the {@code invoice_snapshot} table. */
public interface InvoiceSnapshotJpaRepository extends JpaRepository<InvoiceSnapshotEntity, UUID> {
}
