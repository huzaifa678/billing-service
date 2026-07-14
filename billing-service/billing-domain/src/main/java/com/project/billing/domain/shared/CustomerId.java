package com.project.billing.domain.shared;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a customer. */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "customer id must not be null");
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId of(String value) {
        return new CustomerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
