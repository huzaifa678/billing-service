package com.project.billing_service.exceptions;

public class SubscriptionServiceUnavailableException extends RuntimeException{
    public SubscriptionServiceUnavailableException(String message) {
        super(message);
    }
}
