package com.project.billing.application.shared.port.out;

import com.project.billing.domain.shared.DomainEvent;

import java.util.Collection;

/**
 * Outbound port for publishing domain events drained from aggregates.
 * The adapter decides the delivery mechanism and timing (e.g. after commit).
 */
public interface DomainEventPublisherPort {

    void publishAll(Collection<? extends DomainEvent> events);
}
