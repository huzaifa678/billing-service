package com.project.billing;

import com.project.billing.adapter.out.ratelimit.Bucket4jRateLimiterAdapter;
import com.project.billing.adapter.out.subscription.SubscriptionGrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Context-load smoke test. The gRPC client and bucket4j adapter are mocked because
 * their infrastructure beans (ManagedChannel, ProxyManager) are disabled under the
 * test profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingServiceApplicationTests extends AbstractIntegrationTest {

    @MockitoBean
    private SubscriptionGrpcClient subscriptionGrpcClient;

    @MockitoBean
    private Bucket4jRateLimiterAdapter rateLimiterAdapter;

    @Test
    void contextLoads() {
    }
}
