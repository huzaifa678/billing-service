package com.project.billing.adapter.out.persistence.eventstore;

import com.project.billing.application.shared.port.out.EventStorePort;
import com.project.billing.domain.shared.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA-backed {@link EventStorePort} over the shared {@code billing_event_store}. Appends are
 * numbered {@code expectedVersion + 1 ..} and the unique {@code (aggregate_type, aggregate_id,
 * sequence)} constraint rejects a concurrent writer's clashing insert, surfaced as an
 * {@link OptimisticLockingFailureException}.
 */
@Component
@RequiredArgsConstructor
public class JpaEventStoreAdapter implements EventStorePort {

    private final BillingEventJpaRepository repository;
    private final BillingEventSerializer serializer;

    @Override
    public void append(String aggregateType, UUID aggregateId, long expectedVersion, List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        long sequence = expectedVersion;
        List<BillingEventEntity> rows = new ArrayList<>(events.size());
        for (DomainEvent event : events) {
            sequence++;
            rows.add(BillingEventEntity.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .sequence(sequence)
                    .eventType(serializer.typeOf(event))
                    .payload(serializer.serialize(event))
                    .occurredAt(event.occurredOn())
                    .published(false)
                    .build());
        }

        try {
            repository.saveAll(rows);
            // Flush now so a concurrency clash is reported inside the caller's transaction.
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new OptimisticLockingFailureException(
                    "Concurrent modification of " + aggregateType + " " + aggregateId
                            + " (expected version " + expectedVersion + ")", e);
        }
    }

    @Override
    public List<DomainEvent> loadAfter(String aggregateType, UUID aggregateId, long afterSequence) {
        return repository
                .findByAggregateTypeAndAggregateIdAndSequenceGreaterThanOrderBySequenceAsc(
                        aggregateType, aggregateId, afterSequence)
                .stream()
                .map(row -> serializer.deserialize(row.getEventType(), row.getPayload()))
                .toList();
    }
}
