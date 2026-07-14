package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.shared.DomainEvent;

/**
 * Spring application-event carrier for a domain event, letting the messaging
 * adapter deliver it after the surrounding transaction commits via
 * {@code @TransactionalEventListener}.
 */
public record DomainEventEnvelope(DomainEvent event) {
}
