package com.project.billing.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for event-sourced aggregate roots. Behaviour applies each change as a
 * {@link DomainEvent}: the event is folded into state and recorded. Current state is the
 * fold of the event stream, and {@link #version()} (the count of applied events) is the basis
 * for optimistic concurrency when the event store appends new events. The application layer
 * drains pending events via {@link #pullDomainEvents()} after the aggregate is persisted.
 */
public abstract class AbstractAggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** Number of events applied to this aggregate (0 for a brand-new instance). */
    private long version;

    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Advance the version by one applied event. Aggregates call this from their {@code apply}
     * method so it runs for both freshly-raised events and events replayed from the store.
     */
    protected void markApplied() {
        this.version++;
    }

    /**
     * Restore the version directly when rebuilding from a snapshot (rather than counting a full
     * replay). The repository then applies only the events after the snapshot.
     */
    protected void restoreVersion(long version) {
        this.version = version;
    }

    /** Aggregate version = number of events folded in so far. */
    public long version() {
        return version;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
}
