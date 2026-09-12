package com.project.billing.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link InvoiceJpaEntity}. */
public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    List<InvoiceJpaEntity> findByCustomerId(UUID customerId);

    List<InvoiceJpaEntity> findByStatus(String status);

    Optional<InvoiceJpaEntity> findBySubscriptionId(UUID subscriptionId);

    /**
     * Load an invoice with a {@code SELECT ... FOR UPDATE} row lock, held until the
     * surrounding transaction commits. Used by the payment flow to serialize
     * concurrent pay attempts on the same invoice (retries / double-submit) so they
     * cannot drive two charges or a lost status transition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceJpaEntity i where i.invoiceId = :id")
    Optional<InvoiceJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
