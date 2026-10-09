package com.project.billing.application.usage.service;

import com.project.billing.application.usage.port.in.RecordUsageChargeCommand;
import com.project.billing.application.usage.port.in.RecordUsageChargeUseCase;
import com.project.billing.application.usage.port.out.UsageChargeRepositoryPort;
import com.project.billing.domain.usage.UsageCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records usage charges. The aggregate computes its total and raises {@code UsageChargeCreated};
 * the event-sourced repository appends that event to the store, and the transactional-outbox
 * relay publishes it to Kafka out of band — so the command side no longer publishes directly.
 */
@Service
@RequiredArgsConstructor
public class UsageChargeCommandService implements RecordUsageChargeUseCase {

    private final UsageChargeRepositoryPort repository;

    @Override
    @Transactional
    public UsageCharge recordCharge(RecordUsageChargeCommand command) {
        UsageCharge charge = UsageCharge.create(
                command.invoiceId(),
                command.metric(),
                command.quantity(),
                command.unitPrice()
        );

        return repository.save(charge);
    }
}
