package com.project.billing.application.invoice.service;

import com.project.billing.application.invoice.port.in.CreateInvoiceCommand;
import com.project.billing.application.invoice.port.in.PayInvoiceCommand;
import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.application.invoice.port.out.PaymentPort;
import com.project.billing.application.invoice.port.out.PaymentResult;
import com.project.billing.application.invoice.port.out.RateLimiterPort;
import com.project.billing.application.invoice.port.out.SubscriptionGatewayPort;
import com.project.billing.application.usage.port.in.RecordUsageChargeCommand;
import com.project.billing.application.usage.port.in.RecordUsageChargeUseCase;
import com.project.billing.domain.exception.InvalidSubscriptionStateException;
import com.project.billing.domain.exception.PaymentFailedException;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceCommandServiceTest {

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
    void createInvoice_activeSubscription_persistsAndDoesNotRateLimit() {
        UUID subId = UUID.randomUUID();
        when(subscriptionGateway.getSubscription(any())).thenReturn(snapshot("ACTIVE"));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                subId, UUID.randomUUID(), new BigDecimal("100.00"), "USD",
                "ISSUED", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7)
        );

        Invoice result = service.create(command);

        assertNotNull(result);
        assertEquals(subId, result.subscriptionId().value());
        verify(rateLimiter, never()).checkPayInvoice(any());
    }

    @Test
    void createInvoice_inactiveSubscription_throws() {
        when(subscriptionGateway.getSubscription(any())).thenReturn(snapshot("CANCELED"));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), "USD",
                "ISSUED", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7)
        );

        assertThrows(InvalidSubscriptionStateException.class, () -> service.create(command));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void payInvoice_success_recordsUsageCharge() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = existingInvoice(invoiceId, "100");
        when(invoiceRepository.findById(InvoiceId.of(invoiceId))).thenReturn(Optional.of(invoice));
        when(paymentPort.pay(any(), any())).thenReturn(new PaymentResult("succeeded"));

        String status = service.pay(new PayInvoiceCommand(invoiceId, "pm_123"));

        assertEquals("succeeded", status);
        verify(rateLimiter).checkPayInvoice(InvoiceId.of(invoiceId));
        verify(invoiceRepository).save(argThat(saved -> saved.status() == InvoiceStatus.PAID));
        verify(recordUsageChargeUseCase).record(argThat((RecordUsageChargeCommand c) ->
                c.invoiceId().equals(InvoiceId.of(invoiceId))
                        && c.metric().value().equals("api_calls")
                        && c.quantity() == 1L
                        && c.unitPrice().amount().compareTo(new BigDecimal("100")) == 0
        ));
    }

    @Test
    void payInvoice_failure_marksInvoiceFailedAndThrows() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = existingInvoice(invoiceId, "100");
        when(invoiceRepository.findById(InvoiceId.of(invoiceId))).thenReturn(Optional.of(invoice));
        when(paymentPort.pay(any(), any())).thenThrow(new RuntimeException("payment failed"));

        assertThrows(PaymentFailedException.class,
                () -> service.pay(new PayInvoiceCommand(invoiceId, "pm")));

        verify(rateLimiter).checkPayInvoice(InvoiceId.of(invoiceId));
        verify(invoiceRepository).save(argThat(saved -> saved.status() == InvoiceStatus.FAILED));
        verify(recordUsageChargeUseCase, never()).record(any());
    }

    private static Invoice existingInvoice(UUID invoiceId, String amount) {
        return Invoice.reconstitute(
                InvoiceId.of(invoiceId),
                SubscriptionId.of(UUID.randomUUID()),
                CustomerId.of(UUID.randomUUID()),
                Money.of(amount, "USD"),
                InvoiceStatus.ISSUED,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(7)
        );
    }
}
