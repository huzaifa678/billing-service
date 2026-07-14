package com.project.billing.application.usage.service;

import com.project.billing.application.shared.port.out.DomainEventPublisherPort;
import com.project.billing.application.usage.port.in.RecordUsageChargeCommand;
import com.project.billing.application.usage.port.in.RecordUsageChargeUseCase;
import com.project.billing.application.usage.port.out.UsageChargeRepositoryPort;
import com.project.billing.domain.usage.UsageCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records usage charges. The aggregate computes its total and raises
 * {@code UsageChargeCreated}; this service persists it and publishes the event
 * through the outbound port (delivered after commit by the messaging adapter).
 */
@Service
@RequiredArgsConstructor
public class UsageChargeCommandService implements RecordUsageChargeUseCase {

    private final UsageChargeRepositoryPort repository;
    private final DomainEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public UsageCharge record(RecordUsageChargeCommand command) {
        UsageCharge charge = UsageCharge.create(
                command.invoiceId(),
                command.metric(),
                command.quantity(),
                command.unitPrice()
        );

        UsageCharge saved = repository.save(charge);

        eventPublisher.publishAll(charge.pullDomainEvents());

        return saved;
    }
}
