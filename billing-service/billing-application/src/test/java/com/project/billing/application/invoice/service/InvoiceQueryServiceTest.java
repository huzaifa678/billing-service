package com.project.billing.application.invoice.service;

import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.application.invoice.port.out.SubscriptionGatewayPort;
import com.project.billing.domain.exception.InvoiceNotFoundException;
import com.project.billing.domain.subscription.SubscriptionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceQueryServiceTest {

    @Mock
    private InvoiceRepositoryPort invoiceRepository;
    @Mock
    private SubscriptionGatewayPort subscriptionGateway;

    @InjectMocks
    private InvoiceQueryService service;

    @Test
    void getById_notFound_throws() {
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(InvoiceNotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void activeByUser_returnsList() {
        Instant now = Instant.now();
        when(subscriptionGateway.getActiveSubscriptions(any())).thenReturn(List.of(
                new SubscriptionSnapshot("s", "u", "plan", "ACTIVE", now, now, false, now, now)
        ));

        List<SubscriptionSnapshot> result = service.activeByUser(UUID.randomUUID());

        assertFalse(result.isEmpty());
        assertEquals("ACTIVE", result.get(0).status());
    }
}
