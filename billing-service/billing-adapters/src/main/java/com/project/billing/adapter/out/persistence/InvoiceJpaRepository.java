package com.project.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link InvoiceJpaEntity}. */
public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    List<InvoiceJpaEntity> findByCustomerId(UUID customerId);

    List<InvoiceJpaEntity> findByStatus(String status);

    Optional<InvoiceJpaEntity> findBySubscriptionId(UUID subscriptionId);
}
