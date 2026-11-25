package com.project.billing.application.invoice.service;

import com.project.billing.application.invoice.port.in.CreateInvoiceCommand;
import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.application.invoice.port.out.PaymentPort;
import com.project.billing.application.invoice.port.out.RateLimiterPort;
import com.project.billing.application.invoice.port.out.SubscriptionGatewayPort;
import com.project.billing.application.usage.port.in.RecordUsageChargeUseCase;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.subscription.SubscriptionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A retried create carrying the same Idempotency-Key must return the original invoice. */
@ExtendWith(MockitoExtension.class)
class InvoiceIdempotencyTest {

    @Mock
    private InvoiceRepositoryPort invoiceRepository;
    @Mock
    private PaymentPort paymentPort;
    @Mock
    private SubscriptionGatewayPort subscriptionGateway;
    @Mock
    private RateLimiterPort rateLimiter;
    @Mock
    private RecordUsageChargeUseCase recordUsageChargeUseCase;

    @InjectMocks
    private InvoiceCommandService service;

    private static SubscriptionSnapshot snapshot(String status) {
        Instant now = Instant.now();
        return new SubscriptionSnapshot(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), "plan",
                status, now, now, false, now, now
        );
    }

    @Test
    void createInvoice_sameIdempotencyKey_returnsExistingOnReplay() {
        UUID subId = UUID.randomUUID();
        when(subscriptionGateway.getSubscription(any())).thenReturn(snapshot("ACTIVE"));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                subId, UUID.randomUUID(), new BigDecimal("100.00"), "USD",
                "ISSUED", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), "key-xyz"
        );

        Invoice first = service.create(command);

        // Replay: the deterministic id now resolves to the existing invoice; no second raise.
        when(invoiceRepository.findByIdForUpdate(any())).thenReturn(Optional.of(first));
        Invoice replay = service.create(command);

        assertEquals(first.id(), replay.id());
        verify(invoiceRepository, times(1)).save(any());
    }
}
