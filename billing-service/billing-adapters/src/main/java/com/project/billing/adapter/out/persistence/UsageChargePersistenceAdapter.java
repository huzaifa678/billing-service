package com.project.billing.adapter.out.persistence;

import com.project.billing.application.usage.port.out.UsageChargeProjectionPort;
import com.project.billing.domain.usage.UsageCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Read-model projection for the event-sourced usage-charge aggregate. The event-sourced repository
 * calls {@link #project(UsageCharge)} in the same transaction as the event append to keep the
 * {@code usage_charges} table consistent with the event store.
 */
@Component
@RequiredArgsConstructor
public class UsageChargePersistenceAdapter implements UsageChargeProjectionPort {

    private final UsageChargeJpaRepository jpaRepository;

    @Override
    public void project(UsageCharge charge) {
        jpaRepository.save(new UsageChargeJpaEntity(
                charge.id().value(),
                charge.invoiceId().value(),
                charge.metric().value(),
                charge.quantity(),
                charge.unitPrice().amount(),
                charge.totalPrice().amount(),
                // Create-only aggregate; not-new so Spring Data merges (idempotent upsert).
                false
        ));
    }
}
