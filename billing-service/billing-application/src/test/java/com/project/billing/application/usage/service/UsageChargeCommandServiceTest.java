package com.project.billing.application.usage.service;

import com.project.billing.application.usage.port.in.RecordUsageChargeCommand;
import com.project.billing.application.usage.port.out.UsageChargeRepositoryPort;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.Metric;
import com.project.billing.domain.usage.UsageCharge;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageChargeCommandServiceTest {

    @Mock
    private UsageChargeRepositoryPort repository;

    @InjectMocks
    private UsageChargeCommandService service;

    @Test
    void record_computesTotalAndPersists() {
        InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        UsageCharge result = service.record(new RecordUsageChargeCommand(
                invoiceId, Metric.of("api_calls"), 2L, Money.of("10", "USD")
        ));

        assertEquals(invoiceId, result.invoiceId());
        assertEquals(0, result.totalPrice().amount().compareTo(new BigDecimal("20")));

        // The aggregate raised its creation event; the event-sourced repository (not this service)
        // now appends it to the store and the outbox relay publishes it.
        verify(repository).save(any());
        assertThat(result.domainEvents()).hasSize(1);
        assertThat(result.domainEvents().get(0)).isInstanceOf(UsageChargeCreated.class);
    }
}
