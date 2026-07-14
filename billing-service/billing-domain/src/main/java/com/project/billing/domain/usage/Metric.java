package com.project.billing.domain.usage;

import java.util.Objects;

/** Value object for a usage metric name (e.g. {@code api_calls}, {@code storage_gb}). */
public record Metric(String value) {

    public Metric {
        Objects.requireNonNull(value, "metric must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("metric must not be blank");
        }
    }

    public static Metric of(String value) {
        return new Metric(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
