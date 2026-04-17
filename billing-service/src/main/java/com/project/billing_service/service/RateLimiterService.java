package com.project.billing_service.service;

import com.project.billing_service.config.RateLimitPolicy;
import com.project.billing_service.exceptions.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitPolicy policy;

    public void checkPayInvoice(UUID invoiceId) {
        Bucket bucket = proxyManager.builder()
                .build(
                        "pay-invoice:" + invoiceId,
                        () -> BucketConfiguration.builder()
                                .addLimit(policy.payInvoicePolicy())
                                .build()
                );

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(
                    "Too many payment attempts for invoice " + invoiceId
            );
        }
    }

    public void checkSubscriptionEvent() {
        Bucket bucket = proxyManager.builder()
                .build(
                        "subscription.created",
                        () -> BucketConfiguration.builder()
                                .addLimit(policy.subscriptionEventPolicy())
                                .build()
                );

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(
                    "Subscription event rate limit exceeded"
            );
        }
    }
}

