package com.project.billing.domain.subscription;

import java.time.Instant;

/**
 * Read-model value object describing a subscription as seen by the billing context.
 * Returned by the subscription gateway port so that transport types (the gRPC
 * {@code GetSubscriptionResponse}) never leak into application or domain logic.
 */
public record SubscriptionSnapshot(
        String subscriptionId,
        String userId,
        String planId,
        String status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant createdAt,
        Instant updatedAt
) {

    private static final String ACTIVE_STATUS = "ACTIVE";

    public boolean isActive() {
        return ACTIVE_STATUS.equals(status);
    }
}
