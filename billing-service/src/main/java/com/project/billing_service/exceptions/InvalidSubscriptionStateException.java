package com.project.billing_service.exceptions;

public class InvalidSubscriptionStateException extends RuntimeException{
    public InvalidSubscriptionStateException(String message) {
        super(message);
    }
}
