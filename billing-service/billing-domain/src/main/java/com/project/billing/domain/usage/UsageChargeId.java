package com.project.billing.domain.usage;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a {@link UsageCharge}. */
public record UsageChargeId(UUID value) {

    public UsageChargeId {
        Objects.requireNonNull(value, "usage charge id must not be null");
    }

    public static UsageChargeId of(UUID value) {
        return new UsageChargeId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
