package com.project.billing.domain.shared;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a subscription (owned by the subscription service). */
public record SubscriptionId(UUID value) {

    public SubscriptionId {
        Objects.requireNonNull(value, "subscription id must not be null");
    }

    public static SubscriptionId of(UUID value) {
        return new SubscriptionId(value);
    }

    public static SubscriptionId of(String value) {
        return new SubscriptionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
