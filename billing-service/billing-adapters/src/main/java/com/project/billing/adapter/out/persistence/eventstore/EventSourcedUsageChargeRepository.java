package com.project.billing.adapter.out.persistence.eventstore;

import com.project.billing.application.shared.port.out.EventStorePort;
import com.project.billing.application.usage.port.out.UsageChargeProjectionPort;
import com.project.billing.application.usage.port.out.UsageChargeRepositoryPort;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.usage.UsageCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event-sourced {@link UsageChargeRepositoryPort}. Usage charges are create-only, so {@code save}
 * appends the single {@code UsageChargeCreated} event to the store and projects the read-model row
 * in the same transaction; the outbox relay publishes it to Kafka. No snapshots or replay loads are
 * needed for a create-only aggregate.
 */
@Component
@Primary
@RequiredArgsConstructor
public class EventSourcedUsageChargeRepository implements UsageChargeRepositoryPort {

    static final String AGGREGATE_TYPE = "usage-charge";

    private final EventStorePort eventStore;
    private final UsageChargeProjectionPort projection;

    @Override
    public UsageCharge save(UsageCharge charge) {
        List<DomainEvent> pending = charge.pullDomainEvents();
        if (pending.isEmpty()) {
            return charge;
        }
        long expectedVersion = charge.version() - pending.size();
        eventStore.append(AGGREGATE_TYPE, charge.id().value(), expectedVersion, pending);
        projection.project(charge);
        return charge;
    }
}
