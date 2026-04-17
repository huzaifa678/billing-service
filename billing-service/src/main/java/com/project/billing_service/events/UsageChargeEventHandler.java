package com.project.billing_service.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UsageChargeEventHandler {

    private final UsageChargeEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UsageChargeCreatedEvent event) {
        producer.publish(event.getEntity());
    }
}
