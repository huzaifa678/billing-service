package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.usage.event.UsageChargeCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges domain events to Kafka after the transaction commits, preserving the
 * previous {@code AFTER_COMMIT} publish semantics for usage-charge events.
 */
@Component
@RequiredArgsConstructor
public class UsageChargeCreatedEventHandler {

    private final UsageChargeEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DomainEventEnvelope envelope) {
        if (envelope.event() instanceof UsageChargeCreated event) {
            producer.publish(event);
        }
    }
}
