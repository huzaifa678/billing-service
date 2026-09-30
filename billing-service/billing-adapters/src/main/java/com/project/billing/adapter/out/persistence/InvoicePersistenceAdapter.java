package com.project.billing.adapter.out.persistence;

import com.project.billing.application.invoice.port.out.InvoiceProjectionPort;
import com.project.billing.application.invoice.port.out.InvoiceReadModelPort;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Read-model side of the event-sourced invoice aggregate. Serves queries from the materialised
 * {@code invoices} table ({@link InvoiceReadModelPort}) and lets the event-sourced repository keep
 * that table in step by upserting the aggregate's current state after each append
 * ({@link InvoiceProjectionPort}). It is never the source of truth — that is the event store.
 */
@Component
@RequiredArgsConstructor
public class InvoicePersistenceAdapter implements InvoiceProjectionPort, InvoiceReadModelPort {

    private final InvoiceJpaRepository jpaRepository;
    private final InvoicePersistenceMapper mapper;

    @Override
    public void project(Invoice invoice) {
        // Write-only projection. The mapper marks the entity not-new so Spring Data upserts
        // (merge), keeping the read model in step without deciding insert-vs-update here.
        jpaRepository.save(mapper.toJpa(invoice));
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Invoice> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Invoice> findByStatus(InvoiceStatus status) {
        return jpaRepository.findByStatus(status.name()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Invoice> findBySubscriptionId(SubscriptionId subscriptionId) {
        return jpaRepository.findBySubscriptionId(subscriptionId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdForUpdate(InvoiceId id) {
        return jpaRepository.findByIdForUpdate(id.value()).map(mapper::toDomain);
    }
}
