package com.project.billing.domain.invoice;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying an {@link Invoice}. */
public record InvoiceId(UUID value) {

    public InvoiceId {
        Objects.requireNonNull(value, "invoice id must not be null");
    }

    public static InvoiceId of(UUID value) {
        return new InvoiceId(value);
    }

    public static InvoiceId of(String value) {
        return new InvoiceId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
