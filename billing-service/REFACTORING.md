# Refactoring Notes — billing-service

This document records the SOLID/clean-code refactor of the billing-service
(Spring Boot + gRPC + Kafka/Avro + Stripe), the problems that existed before,
why each one mattered, and how it was fixed.

No test method bodies were changed. After the refactor the full suite is green:
**14 tests, 0 failures, BUILD SUCCESSFUL.**

## What was added

| File | Role | Pattern |
|------|------|---------|
| `service/SubscriptionGateway.java` | Port to the subscription service | Gateway (port) |
| `client/SubscriptionGrpcGateway.java` | gRPC adapter + failure translation | Adapter |
| `service/SubscriptionUpdateHandler.java` | Subscription→invoice state machine | — (SRP extraction) |
| `test/.../AbstractIntegrationTest.java` | Shared Testcontainers fixture | Base-class test pattern |

The existing **Strategy** pattern (`BillingInterface` / `StripePayment`) was kept.

---

## 1. DIP + DRY — concrete gRPC dependency and duplicated failure handling

`BillingService` depended directly on the concrete `SubscriptionGrpcClient`, and
the same `try/catch → SubscriptionServiceUnavailableException` translation was
written twice (in `createInvoice` and `getActiveSubscriptions`).

### Before

```java
private final SubscriptionGrpcClient subscriptionGrpcClient;

public InvoiceEntity createInvoice(InvoiceDto dto) {
    GetSubscriptionResponse sub;
    try {
        sub = subscriptionGrpcClient.getSubscription(dto.getSubscriptionId().toString());
    } catch (Exception ex) {
        throw new SubscriptionServiceUnavailableException("Subscription service is unavailable");
    }
    // ...
}

public List<GetSubscriptionResponse> getActiveSubscriptions(UUID userId) {
    try {
        GetUserActiveSubscriptionsResponse response =
                subscriptionGrpcClient.getUserActiveSubscriptions(userId.toString());
        return response.getSubscriptionsList();
    } catch (Exception ex) {                                    // same block again
        throw new SubscriptionServiceUnavailableException("Subscription service is unavailable");
    }
}
```

### After

```java
// service/SubscriptionGateway.java  (port — high-level module depends on this)
public interface SubscriptionGateway {
    GetSubscriptionResponse getSubscription(String subscriptionId);
    List<GetSubscriptionResponse> getActiveSubscriptions(String userId);
}

// client/SubscriptionGrpcGateway.java  (adapter — owns transport concerns)
@Component
@RequiredArgsConstructor
public class SubscriptionGrpcGateway implements SubscriptionGateway {
    private final SubscriptionGrpcClient client;

    @Override
    public GetSubscriptionResponse getSubscription(String subscriptionId) {
        try {
            return client.getSubscription(subscriptionId);
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException("Subscription service is unavailable");
        }
    }

    @Override
    public List<GetSubscriptionResponse> getActiveSubscriptions(String userId) {
        try {
            return client.getUserActiveSubscriptions(userId).getSubscriptionsList();
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException("Subscription service is unavailable");
        }
    }
}
```

```java
// BillingService now depends on the abstraction and reads cleanly
private final SubscriptionGateway subscriptionGateway;

public InvoiceEntity createInvoice(InvoiceDto dto) {
    GetSubscriptionResponse sub =
            subscriptionGateway.getSubscription(dto.getSubscriptionId().toString());
    if (!ACTIVE_STATUS.equals(sub.getStatus())) {
        throw new InvalidSubscriptionStateException("Cannot create invoice for inactive subscription");
    }
    return invoiceRepository.save(mapper.convertToEntity(dto));
}

public List<GetSubscriptionResponse> getActiveSubscriptions(UUID userId) {
    return subscriptionGateway.getActiveSubscriptions(userId.toString());
}
```

---

## 2. SRP — a subscription state machine inside the billing service

`handleSubscriptionUpdate` mapped a subscription status onto an invoice status —
a distinct responsibility that did not belong in the billing service.

### Before

```java
@Transactional
public void handleSubscriptionUpdate(SubscriptionUpdated event) {
    UUID subscriptionId = UUID.fromString(event.getSubscriptionId());
    SubscriptionStatus status = event.getStatus();
    switch (status) {
        case ACTIVE -> invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(invoice -> {
                    if (InvoiceStatus.DRAFT.name().equals(invoice.getStatus())) {
                        invoice.setStatus(InvoiceStatus.ISSUED.name());
                    }
                });
        case CANCELED, EXPIRED -> invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(invoice -> {
                    invoice.setStatus(InvoiceStatus.FAILED.name());
                    invoiceRepository.save(invoice);
                });
        case TRIALING -> { }
    }
}
```

### After

```java
// service/SubscriptionUpdateHandler.java
@Component
@RequiredArgsConstructor
public class SubscriptionUpdateHandler {
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(SubscriptionUpdated event) {
        UUID subscriptionId = UUID.fromString(event.getSubscriptionId());
        switch (event.getStatus()) {
            case ACTIVE -> activateInvoice(subscriptionId);
            case CANCELED, EXPIRED -> failInvoice(subscriptionId);
            case TRIALING -> { /* no invoice change while trialing */ }
        }
    }

    private void activateInvoice(UUID subscriptionId) {
        invoiceRepository.findBySubscriptionId(subscriptionId).ifPresent(invoice -> {
            if (InvoiceStatus.DRAFT.name().equals(invoice.getStatus())) {
                invoice.setStatus(InvoiceStatus.ISSUED.name());
            }
        });
    }

    private void failInvoice(UUID subscriptionId) {
        invoiceRepository.findBySubscriptionId(subscriptionId).ifPresent(invoice -> {
            invoice.setStatus(InvoiceStatus.FAILED.name());
            invoiceRepository.save(invoice);
        });
    }
}

// the consumer routes straight to the handler now
@KafkaListener(topics = "subscription.updated", groupId = "billing-service")
public void handleSubscriptionUpdated(SubscriptionUpdated event) {
    subscriptionUpdateHandler.handle(event);
}
```

---

## 3. Clean code — magic values in `payInvoice`

### Before

```java
String status = paymentService.payInvoice(invoice, paymentMethodId);
if ("succeeded".equals(status)) {
    String metric = "api_calls";
    long quantity = 1L;
    BigDecimal unitPrice = invoice.getAmount().divide(BigDecimal.valueOf(quantity));
    usageChargeService.createUsageCharge(invoice.getInvoiceId(), metric, quantity, unitPrice);
}
```

### After

```java
private static final String ACTIVE_STATUS = "ACTIVE";
private static final String PAYMENT_SUCCEEDED = "succeeded";
private static final String USAGE_METRIC_API_CALLS = "api_calls";
private static final long USAGE_QUANTITY = 1L;

String status = paymentService.payInvoice(invoice, paymentMethodId);
if (PAYMENT_SUCCEEDED.equals(status)) {
    recordUsageCharge(invoice);
}
// ...
private void recordUsageCharge(InvoiceEntity invoice) {
    BigDecimal unitPrice = invoice.getAmount().divide(BigDecimal.valueOf(USAGE_QUANTITY));
    usageChargeService.createUsageCharge(
            invoice.getInvoiceId(), USAGE_METRIC_API_CALLS, USAGE_QUANTITY, unitPrice);
}
```

---

## 4. Tests — shared integration base class (enterprise pattern)

`BillingServiceTest` and `BillingServiceApplicationTests` each repeated the same
three Testcontainers (Kafka, PostgreSQL, Redis) plus the same profile/database
annotations.

### Before

```java
@SpringBootTest
@Import(GrpcTestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class BillingServiceTest {

    @ServiceConnection @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("test").withUsername("test").withPassword("test");

    @Container @ServiceConnection
    static final GenericContainer redis =
            new GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    // ...mocks + tests
}
// BillingServiceApplicationTests duplicated the exact same container block
```

### After

```java
// AbstractIntegrationTest.java — owns the shared infrastructure
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractIntegrationTest {

    @ServiceConnection @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    @ServiceConnection @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("test").withUsername("test").withPassword("test");

    @ServiceConnection @Container
    static final GenericContainer REDIS =
            new GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
}

@SpringBootTest
@Import(GrpcTestConfig.class)
class BillingServiceTest extends AbstractIntegrationTest { /* mocks + tests only */ }

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingServiceApplicationTests extends AbstractIntegrationTest { /* mocks only */ }
```

`@SpringBootTest` stays on each subclass so `BillingServiceApplicationTests`
keeps its `RANDOM_PORT` web environment; everything shared moves to the base.

---

## 5. Build — OpenTelemetry version conflict (the suite-wide blocker)

The whole integration suite failed before this fix. The logback appender was
pinned to a version built against an old OTel API, while Spring Boot 4.0.6
resolves OTel **API 1.55.0**. The mismatch threw
`NoClassDefFoundError: SemconvStability` /
`NoSuchMethodError: LogRecordBuilder.setException` on the first log line of any
Spring-context test, cascading across the shared test JVM.

### Before

```gradle
implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.28.1-alpha'
```

### After

```gradle
// Aligned with the OpenTelemetry API (1.55.0) managed by Spring Boot's
// opentelemetry starter: instrumentation 2.N.0 tracks API 1.(34+N).
implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha'
```

---

## Outcome

- `BillingService` depends on an abstraction; transport failure-handling lives in
  one adapter; the subscription state machine is its own component.
- Payment magic values are named constants.
- Integration tests share one container fixture instead of duplicating it.
- The OpenTelemetry versions are aligned, so the suite runs.
- **14 tests pass, BUILD SUCCESSFUL.**
```
BillingControllerTest .............. 1 ✅
BillingServiceApplicationTests ..... 1 ✅
BillingServiceTest ................. 6 ✅
RateLimiterTest .................... 3 ✅
UsageServiceTest ................... 1 ✅
SubscriptionGrpcClientPactTest ..... 2 ✅
```
