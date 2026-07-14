package com.project.billing.application.invoice.port.in;

import java.util.UUID;

/** Command carrying a subscription status change that may affect its invoice. */
public record SubscriptionUpdateCommand(UUID subscriptionId, SubscriptionLifecycleStatus status) {
}
