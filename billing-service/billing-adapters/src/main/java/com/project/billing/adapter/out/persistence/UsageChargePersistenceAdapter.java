package com.project.billing.adapter.out.persistence;

import com.project.billing.application.usage.port.out.UsageChargeRepositoryPort;
import com.project.billing.domain.usage.UsageCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA-backed implementation of {@link UsageChargeRepositoryPort}. Usage charges
 * are only ever created (never reloaded), so identity is already assigned by the
 * aggregate and the input is returned unchanged after persisting.
 */
@Component
@RequiredArgsConstructor
public class UsageChargePersistenceAdapter implements UsageChargeRepositoryPort {

    private final UsageChargeJpaRepository jpaRepository;

    @Override
    public UsageCharge save(UsageCharge charge) {
        jpaRepository.save(new UsageChargeJpaEntity(
                charge.id().value(),
                charge.invoiceId().value(),
                charge.metric().value(),
                charge.quantity(),
                charge.unitPrice().amount(),
                charge.totalPrice().amount(),
                charge.isNew()
        ));
        return charge;
    }
}
