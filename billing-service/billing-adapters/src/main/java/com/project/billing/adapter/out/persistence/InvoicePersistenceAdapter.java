package com.project.billing.adapter.out.persistence;

import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** JPA-backed implementation of {@link InvoiceRepositoryPort}. */
@Component
@RequiredArgsConstructor
public class InvoicePersistenceAdapter implements InvoiceRepositoryPort {

    private final InvoiceJpaRepository jpaRepository;
    private final InvoicePersistenceMapper mapper;

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity saved = jpaRepository.save(mapper.toJpa(invoice));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findBySubscriptionId(SubscriptionId subscriptionId) {
        return jpaRepository.findBySubscriptionId(subscriptionId.value()).map(mapper::toDomain);
    }

    @Override
    public List<Invoice> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Invoice> findByStatus(InvoiceStatus status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
