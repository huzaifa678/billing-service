package com.project.billing_service.repository;

import com.project.billing_service.model.entities.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository
        extends JpaRepository<InvoiceEntity, UUID> {

    List<InvoiceEntity> findByCustomerId(UUID customerId);

    List<InvoiceEntity> findByStatus(String status);

    Optional<InvoiceEntity> findBySubscriptionId(UUID subscriptionId);

    void deleteAll();
}
