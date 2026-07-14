package com.project.billing.application.invoice.port.out;

/** Result of a payment attempt returned by the {@link PaymentPort}. */
public record PaymentResult(String status) {

    private static final String SUCCEEDED = "succeeded";

    public boolean isSuccessful() {
        return SUCCEEDED.equals(status);
    }
}
