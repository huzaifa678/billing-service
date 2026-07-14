package com.project.billing.application.invoice.port.in;

/** Inbound port: react to a subscription status change (activate / fail its invoice). */
public interface HandleSubscriptionUpdateUseCase {

    void handle(SubscriptionUpdateCommand command);
}
