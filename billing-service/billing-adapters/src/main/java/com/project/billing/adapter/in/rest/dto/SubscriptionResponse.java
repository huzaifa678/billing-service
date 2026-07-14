package com.project.billing.adapter.in.rest.dto;

import java.time.Instant;

/** REST response body for a subscription returned to billing clients. */
public record SubscriptionResponse(
        String id,
        String userId,
        String planId,
        String status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant createdAt,
        Instant updatedAt
) {
}
