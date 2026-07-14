package com.project.billing.adapter.in.messaging;

import billing.SubscriptionUpdated;
import com.project.billing.application.invoice.port.in.HandleSubscriptionUpdateUseCase;
import com.project.billing.application.invoice.port.in.SubscriptionLifecycleStatus;
import com.project.billing.application.invoice.port.in.SubscriptionUpdateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Consumes {@code subscription.updated} and activates/fails the related invoice. */
@Component
@RequiredArgsConstructor
public class SubscriptionUpdatedEventConsumer {

    private final HandleSubscriptionUpdateUseCase handleSubscriptionUpdateUseCase;

    @KafkaListener(
            topics = "subscription.updated",
            groupId = "billing-service"
    )
    public void handleSubscriptionUpdated(SubscriptionUpdated event) {
        UUID subscriptionId = UUID.fromString(event.getSubscriptionId());
        SubscriptionLifecycleStatus status =
                SubscriptionLifecycleStatus.valueOf(event.getStatus().name());

        handleSubscriptionUpdateUseCase.handle(
                new SubscriptionUpdateCommand(subscriptionId, status)
        );
    }
}
