package com.project.billing_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.billing_service.client.SubscriptionGrpcClient;
import com.project.billing_service.service.BillingInterface;
import com.project.billing_service.service.RateLimiterService;
import com.project.billing_service.service.UsageChargeService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingServiceApplicationTests extends AbstractIntegrationTest {

    @MockitoBean
    private SubscriptionGrpcClient subscriptionGrpcClient;

    @MockitoBean
    private BillingInterface paymentService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private UsageChargeService usageChargeService;

    @Test
    void contextLoads() {
    }
}
