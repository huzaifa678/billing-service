package com.project.billing.adapter.in.rest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ErrorResponse {

    private final int status;
    private final String message;
    private final LocalDateTime timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
