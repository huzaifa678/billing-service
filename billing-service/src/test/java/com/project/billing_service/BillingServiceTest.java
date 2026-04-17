package com.project.billing_service;

import com.project.billing_service.config.GrpcTestConfig;
import com.project.billing_service.client.SubscriptionGrpcClient;
import com.project.billing_service.model.dtos.InvoiceDto;
import com.project.billing_service.model.entities.InvoiceEntity;
import com.project.billing_service.repository.InvoiceRepository;
import com.project.billing_service.service.BillingInterface;
import com.project.billing_service.service.BillingService;
import com.project.billing_service.service.RateLimiterService;
import com.project.billing_service.service.UsageChargeService;
import subscription.Subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(GrpcTestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class BillingServiceTest {

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

    @Autowired
    private BillingService billingService;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private SubscriptionGrpcClient subscriptionGrpcClient;

    @MockitoBean
    private BillingInterface paymentService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private UsageChargeService usageChargeService;

//    @Autowired
//    JdbcConnectionDetails jdbcConnectionDetails;

    @BeforeEach
    void setUp() {
        when(invoiceRepository.save(any())).thenAnswer(invocation -> {
            InvoiceEntity invoice = invocation.getArgument(0);
            if (invoice.getInvoiceId() == null) {
                invoice.setInvoiceId(UUID.randomUUID());
            }
            return invoice;
        });

        when(subscriptionGrpcClient.getSubscription(anyString()))
                .thenReturn(Subscription.SubscriptionResponse.newBuilder()
                        .setStatus("ACTIVE")
                        .build());

        when(subscriptionGrpcClient.getUserActiveSubscriptions(anyString()))
                .thenReturn(Subscription.GetUserActiveSubscriptionsResponse.newBuilder()
                        .addSubscriptions(Subscription.SubscriptionResponse.newBuilder()
                                .setStatus("ACTIVE")
                                .build())
                        .build());
    }

    @Test
    void createInvoice_success_realGrpc() {

        UUID subId = UUID.randomUUID();

        InvoiceDto dto = new InvoiceDto();
        dto.setSubscriptionId(subId);
        dto.setCustomerId(UUID.randomUUID());
        dto.setAmount(new java.math.BigDecimal("100.00"));
        dto.setCurrency("USD");
        dto.setStatus("ISSUED");
        dto.setIssuedAt(java.time.OffsetDateTime.now());
        dto.setDueAt(java.time.OffsetDateTime.now().plusDays(7));

        InvoiceEntity result = billingService.createInvoice(dto);

        assertNotNull(result);
        assertNotNull(result.getInvoiceId());
        assertEquals(subId, result.getSubscriptionId());

        verify(rateLimiterService, never()).checkPayInvoice(any());
    }

    @Test
    void createInvoice_inactiveSubscription_shouldThrow() {

        InvoiceDto dto = new InvoiceDto();
        dto.setSubscriptionId(UUID.randomUUID());

        when(subscriptionGrpcClient.getSubscription(anyString()))
                .thenReturn(Subscription.SubscriptionResponse.newBuilder()
                        .setStatus("CANCELED")
                        .build());

        assertThrows(RuntimeException.class,
                () -> billingService.createInvoice(dto));

        verify(rateLimiterService, never()).checkPayInvoice(any());
    }

    @Test
    void getInvoice_notFound_shouldThrow() {

        UUID id = UUID.randomUUID();

        when(invoiceRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> billingService.getInvoice(id));
    }

    @Test
    void payInvoice_success_shouldCreateUsageCharge() throws Exception {

        UUID invoiceId = UUID.randomUUID();

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceId(invoiceId);
        invoice.setAmount(new BigDecimal("100"));

        when(invoiceRepository.findById(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(paymentService.payInvoice(any(), any()))
                .thenReturn("succeeded");

        String result = billingService.payInvoice(invoiceId, "pm_123");

        assertEquals("succeeded", result);

        verify(rateLimiterService).checkPayInvoice(invoiceId);

        verify(usageChargeService).createUsageCharge(
                eq(invoiceId),
                eq("api_calls"),
                eq(1L),
                any()
        );
    }

    @Test
    void payInvoice_failure_shouldMarkFailed() throws Exception {

        UUID invoiceId = UUID.randomUUID();

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceId(invoiceId);

        when(invoiceRepository.findById(invoiceId))
                .thenReturn(Optional.of(invoice));

        when(paymentService.payInvoice(any(), any()))
                .thenThrow(new RuntimeException("payment failed"));

        assertThrows(RuntimeException.class,
                () -> billingService.payInvoice(invoiceId, "pm"));

        verify(rateLimiterService).checkPayInvoice(invoiceId);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void getActiveSubscriptions_shouldReturnList() {

        var result = billingService.getActiveSubscriptions(UUID.randomUUID());

        assertFalse(result.isEmpty());
    }
}
