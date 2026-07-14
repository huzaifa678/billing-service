package com.project.billing.adapter.out.ratelimit;

import com.project.billing.adapter.out.ratelimit.config.RateLimitPolicy;
import com.project.billing.application.exception.RateLimitExceededException;
import com.project.billing.application.invoice.port.out.RateLimiterPort;
import com.project.billing.domain.invoice.InvoiceId;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Redis-backed (bucket4j) implementation of {@link RateLimiterPort}. */
@Component
@RequiredArgsConstructor
public class Bucket4jRateLimiterAdapter implements RateLimiterPort {

    private final ProxyManager<String> proxyManager;
    private final RateLimitPolicy policy;

    @Override
    public void checkPayInvoice(InvoiceId invoiceId) {
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

    @Override
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
