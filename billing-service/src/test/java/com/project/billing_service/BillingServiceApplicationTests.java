package com.project.billing_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.project.billing_service.client.SubscriptionGrpcClient;
import com.project.billing_service.service.BillingInterface;
import com.project.billing_service.service.RateLimiterService;
import com.project.billing_service.service.UsageChargeService;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BillingServiceApplicationTests {

    @ServiceConnection
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    @ServiceConnection
    static final GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);

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
