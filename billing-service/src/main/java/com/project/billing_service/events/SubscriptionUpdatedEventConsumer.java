package com.project.billing_service.events;

import billing.SubscriptionUpdated;
import com.project.billing_service.service.SubscriptionUpdateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionUpdatedEventConsumer {
    private final SubscriptionUpdateHandler subscriptionUpdateHandler;

    @KafkaListener(
            topics = "subscription.updated",
            groupId = "billing-service"
    )
    public void handleSubscriptionUpdated(SubscriptionUpdated event) {
        subscriptionUpdateHandler.handle(event);
    }
}
