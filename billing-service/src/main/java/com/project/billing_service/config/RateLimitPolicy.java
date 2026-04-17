package com.project.billing_service.config;


import io.github.bucket4j.Bandwidth;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitPolicy {
    public Bandwidth payInvoicePolicy() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
    }

    public Bandwidth subscriptionEventPolicy() {
        return Bandwidth.builder()
                .capacity(50)
                .refillIntervally(50, Duration.ofMinutes(1))
                .build();
    }
}
