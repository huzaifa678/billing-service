package com.project.billing_service;

import com.project.billing_service.config.RateLimitPolicy;
import com.project.billing_service.exceptions.RateLimitExceededException;
import com.project.billing_service.service.RateLimiterService;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.mockito.ArgumentMatchers.any;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Testcontainers
@ActiveProfiles("test")
public class RateLimiterTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("test")
                    .withUsername("test")
                    .withPassword("test");

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RateLimitPolicy policy;

    @Mock
    private RemoteBucketBuilder<String> bucketBuilder;

    @Mock
    private BucketProxy bucket;

    @InjectMocks
    private RateLimiterService service;

    @BeforeEach
    void setUp() {

        when(proxyManager.builder()).thenReturn(bucketBuilder);

        when(bucketBuilder.build(anyString(), any(Supplier.class)))
                .thenReturn(bucket);
    }

    @Test
    void checkPayInvoice_shouldAllow_whenTokensAvailable() {

        UUID invoiceId = UUID.randomUUID();
        when(bucket.tryConsume(1)).thenReturn(true);

        assertDoesNotThrow(() -> service.checkPayInvoice(invoiceId));

        verify(bucketBuilder).build(eq("pay-invoice:" + invoiceId), any(Supplier.class));
        verify(bucket).tryConsume(1);
    }

    @Test
    void checkPayInvoice_shouldThrow_whenRateLimitExceeded() {

        UUID invoiceId = UUID.randomUUID();
        when(bucket.tryConsume(1)).thenReturn(false);

        assertThrows(RateLimitExceededException.class,
                () -> service.checkPayInvoice(invoiceId));
    }

    @Test
    void checkSubscriptionEvent_shouldThrow_whenRateLimitExceeded() {

        when(bucket.tryConsume(1)).thenReturn(false);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> service.checkSubscriptionEvent());

        assertEquals("Subscription event rate limit exceeded", exception.getMessage());
        verify(bucketBuilder).build(eq("subscription.created"), any(Supplier.class));
    }
}


