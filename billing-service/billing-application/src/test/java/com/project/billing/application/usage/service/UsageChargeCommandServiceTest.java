package com.project.billing.application.usage.service;

import com.project.billing.application.shared.port.out.DomainEventPublisherPort;
import com.project.billing.application.usage.port.in.RecordUsageChargeCommand;
import com.project.billing.application.usage.port.out.UsageChargeRepositoryPort;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.Metric;
import com.project.billing.domain.usage.UsageCharge;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageChargeCommandServiceTest {

    @Mock
    private UsageChargeRepositoryPort repository;
    @Mock
    private DomainEventPublisherPort eventPublisher;

    @Captor
    private ArgumentCaptor<Collection<? extends DomainEvent>> eventsCaptor;

    @InjectMocks
    private UsageChargeCommandService service;

    @Test
    void record_computesTotalPersistsAndPublishesEvent() {
        InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        UsageCharge result = service.record(new RecordUsageChargeCommand(
                invoiceId, Metric.of("api_calls"), 2L, Money.of("10", "USD")
        ));

        assertEquals(invoiceId, result.invoiceId());
        assertEquals(0, result.totalPrice().amount().compareTo(new BigDecimal("20")));

        verify(repository).save(any());
        verify(eventPublisher).publishAll(eventsCaptor.capture());

        Collection<? extends DomainEvent> events = eventsCaptor.getValue();
        assertEquals(1, events.size());
        DomainEvent event = events.iterator().next();
        assertInstanceOf(UsageChargeCreated.class, event);
        UsageChargeCreated created = (UsageChargeCreated) event;
        assertEquals(invoiceId, created.invoiceId());
        assertEquals("api_calls", created.metric().value());
        assertEquals(0, created.unitPrice().amount().compareTo(new BigDecimal("10")));
    }
}
