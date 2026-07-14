package com.project.billing.adapter.out.messaging;

import com.project.billing.application.shared.port.out.DomainEventPublisherPort;
import com.project.billing.domain.shared.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Publishes domain events onto Spring's application-event bus wrapped in a
 * {@link DomainEventEnvelope}, so downstream listeners can react after commit.
 */
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publishAll(Collection<? extends DomainEvent> events) {
        events.forEach(event -> publisher.publishEvent(new DomainEventEnvelope(event)));
    }
}
