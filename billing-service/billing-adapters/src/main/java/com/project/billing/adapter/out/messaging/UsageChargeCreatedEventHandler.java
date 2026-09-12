package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.usage.event.UsageChargeCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges domain events to Kafka after the transaction commits, preserving the
 * previous {@code AFTER_COMMIT} publish semantics for usage-charge events.
 *
 * Runs on the {@code eventTaskExecutor} ({@code @Async}) so the Kafka publish
 * (Avro serialization + broker round-trip on failure paths) is lifted off the
 * thread that committed the payment/usage transaction. Safe because it fires
 * AFTER_COMMIT — the data is already durable, and a publish failure is logged
 * for replay by the producer rather than affecting the committed transaction.
 */
@Component
@RequiredArgsConstructor
public class UsageChargeCreatedEventHandler {

    private final UsageChargeEventProducer producer;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DomainEventEnvelope envelope) {
        if (envelope.event() instanceof UsageChargeCreated event) {
            producer.publish(event);
        }
    }
}
