package com.project.billing.adapter.out.ratelimit;

import com.project.billing.adapter.out.ratelimit.config.RateLimitPolicy;
import com.project.billing.application.exception.RateLimitExceededException;
import com.project.billing.domain.invoice.InvoiceId;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Bucket4jRateLimiterAdapterTest {

    @Mock
    private ProxyManager<String> proxyManager;
    @Mock
    private RateLimitPolicy policy;
    @Mock
    private RemoteBucketBuilder<String> bucketBuilder;
    @Mock
    private BucketProxy bucket;

    @InjectMocks
    private Bucket4jRateLimiterAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkPayInvoice_allowsWhenTokensAvailable() {
        InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID());
        when(bucket.tryConsume(1)).thenReturn(true);

        assertDoesNotThrow(() -> adapter.checkPayInvoice(invoiceId));

        verify(bucketBuilder).build(eq("pay-invoice:" + invoiceId), any(Supplier.class));
        verify(bucket).tryConsume(1);
    }

    @Test
    void checkPayInvoice_throwsWhenRateLimitExceeded() {
        InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID());
        when(bucket.tryConsume(1)).thenReturn(false);

        assertThrows(RateLimitExceededException.class, () -> adapter.checkPayInvoice(invoiceId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkSubscriptionEvent_throwsWhenRateLimitExceeded() {
        when(bucket.tryConsume(1)).thenReturn(false);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> adapter.checkSubscriptionEvent());

        assertEquals("Subscription event rate limit exceeded", exception.getMessage());
        verify(bucketBuilder).build(eq("subscription.created"), any(Supplier.class));
    }
}
