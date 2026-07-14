package com.project.billing.adapter.in.messaging;

import billing.SubscriptionCreated;
import com.project.billing.application.invoice.port.in.CreateInitialInvoiceCommand;
import com.project.billing.application.invoice.port.in.CreateInitialInvoiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Consumes {@code subscription.created} and creates the initial invoice. */
@Component
@RequiredArgsConstructor
public class SubscriptionCreatedEventConsumer {

    private final CreateInitialInvoiceUseCase createInitialInvoiceUseCase;

    @KafkaListener(
            topics = "subscription.created",
            groupId = "billing-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSubscriptionCreated(SubscriptionCreated event) {
        UUID subscriptionId = UUID.fromString(event.getSubscriptionId());
        UUID customerId = UUID.fromString(event.getUserId());

        createInitialInvoiceUseCase.createInitial(
                new CreateInitialInvoiceCommand(subscriptionId, customerId)
        );
    }
}
