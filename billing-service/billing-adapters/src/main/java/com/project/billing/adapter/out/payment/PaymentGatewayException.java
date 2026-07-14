package com.project.billing.adapter.out.payment;

/** Raised when the payment provider fails; treated as a payment failure by the use case. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
