package com.project.billing_service.events;

import billing.SubscriptionCreated;
import com.project.billing_service.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class SubscriptionCreatedEventConsumer {
    private final BillingService billingService;

    @KafkaListener(
            topics = "subscription.created",
            groupId = "billing-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSubscriptionCreated(SubscriptionCreated event) {
        UUID subscriptionId = UUID.fromString((String) event.get("subscriptionId"));
        UUID customerId = UUID.fromString((String) event.get("userId"));

        billingService.createInitialInvoice(subscriptionId, customerId);
    }
}
