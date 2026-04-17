package com.project.billing_service.service;

import com.project.billing_service.events.UsageChargeCreatedEvent;
import com.project.billing_service.model.entities.UsageChargeEntity;
import com.project.billing_service.repository.UsageChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsageChargeService {

    private final UsageChargeRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UsageChargeEntity createUsageCharge(
            UUID invoiceId,
            String metric,
            long quantity,
            BigDecimal unitPrice
    ) {
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        UsageChargeEntity entity = new UsageChargeEntity();
        entity.setInvoiceId(invoiceId);
        entity.setMetric(metric);
        entity.setQuantity(quantity);
        entity.setUnitPrice(unitPrice);
        entity.setTotalPrice(totalPrice);

        UsageChargeEntity saved = repository.save(entity);

        eventPublisher.publishEvent(new UsageChargeCreatedEvent(saved));

        return saved;
    }
}
