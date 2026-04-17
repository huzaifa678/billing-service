package com.project.billing_service.repository;

import com.project.billing_service.model.entities.UsageChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface UsageChargeRepository
        extends JpaRepository<UsageChargeEntity, UUID> {

    List<UsageChargeEntity> findByInvoiceId(UUID invoiceId);
}

