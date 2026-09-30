package com.project.billing.application.shared.port.out;

import com.project.billing.domain.shared.DomainEvent;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the shared append-only event store — the write-side source of truth for
 * every event-sourced aggregate in the service (invoices, usage charges). Rows are keyed by
 * {@code (aggregateType, aggregateId, sequence)}; that uniqueness is the optimistic-concurrency
 * guard on append. Implemented in billing-adapters over {@code billing_event_store}.
 */
public interface EventStorePort {

    /**
     * Append {@code events} for an aggregate, asserting the stream is currently at
     * {@code expectedVersion} (0 for a new stream). The store assigns each event its sequence
     * ({@code expectedVersion + 1 ..}) and a unique event id. A concurrent writer's clash surfaces
     * as an {@link org.springframework.dao.OptimisticLockingFailureException}.
     */
    void append(String aggregateType, UUID aggregateId, long expectedVersion, List<DomainEvent> events);

    /** Load an aggregate's events whose sequence is strictly greater than {@code afterSequence}, in order. */
    List<DomainEvent> loadAfter(String aggregateType, UUID aggregateId, long afterSequence);
}
