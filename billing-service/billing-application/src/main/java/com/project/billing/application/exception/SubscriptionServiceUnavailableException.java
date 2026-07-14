package com.project.billing.application.exception;

public class SubscriptionServiceUnavailableException extends RuntimeException {
    public SubscriptionServiceUnavailableException(String message) {
        super(message);
    }
}
