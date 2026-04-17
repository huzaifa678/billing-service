package com.project.billing_service;

import com.project.billing_service.events.UsageChargeCreatedEvent;
import com.project.billing_service.events.UsageChargeEventProducer;
import com.project.billing_service.model.entities.UsageChargeEntity;
import com.project.billing_service.repository.UsageChargeRepository;
import com.project.billing_service.service.UsageChargeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageChargeRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UsageChargeService service;

    @Test
    void shouldCreateUsageCharge() {

        UUID invoiceId = UUID.randomUUID();

        UsageChargeEntity saved = new UsageChargeEntity();
        saved.setId(UUID.randomUUID());
        saved.setInvoiceId(invoiceId);
        saved.setMetric("api_calls");
        saved.setQuantity(1L);
        saved.setUnitPrice(new BigDecimal("10"));
        saved.setTotalPrice(new BigDecimal("10"));

        when(repository.save(any(UsageChargeEntity.class)))
                .thenReturn(saved);

        UsageChargeEntity result = service.createUsageCharge(
                invoiceId,
                "api_calls",
                1L,
                new BigDecimal("10")
        );

        assertNotNull(result);
        assertEquals(invoiceId, result.getInvoiceId());

        verify(repository).save(any(UsageChargeEntity.class));

        ArgumentCaptor<UsageChargeCreatedEvent> captor =
                ArgumentCaptor.forClass(UsageChargeCreatedEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        UsageChargeCreatedEvent event = captor.getValue();

        assertNotNull(event);
        assertNotNull(event.getEntity());
        assertEquals(invoiceId, event.getEntity().getInvoiceId());
        assertEquals("api_calls", event.getEntity().getMetric());
        assertEquals(0, new BigDecimal("10")
                .compareTo(event.getEntity().getUnitPrice()));
    }
}
