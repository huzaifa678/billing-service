package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;
import com.project.billing.domain.subscription.SubscriptionSnapshot;

import java.util.List;

/** Outbound port for reading subscription state from the subscription service. */
public interface SubscriptionGatewayPort {

    SubscriptionSnapshot getSubscription(SubscriptionId subscriptionId);

    List<SubscriptionSnapshot> getActiveSubscriptions(CustomerId customerId);
}
