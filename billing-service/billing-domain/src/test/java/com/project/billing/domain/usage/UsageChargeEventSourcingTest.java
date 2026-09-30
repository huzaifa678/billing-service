package com.project.billing.domain.usage;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the create-only UsageCharge aggregate raises and replays its single event correctly. */
class UsageChargeEventSourcingTest {

    private static UsageCharge charge() {
        return UsageCharge.create(
                InvoiceId.of(UUID.randomUUID()), Metric.of("api_calls"), 3L, Money.of("2", "USD"));
    }

    @Test
    void createRaisesCreatedAndComputesTotalAndVersion() {
        UsageCharge charge = charge();

        assertThat(charge.version()).isEqualTo(1L);
        assertThat(charge.totalPrice().amount()).isEqualByComparingTo("6");
        List<DomainEvent> events = charge.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(UsageChargeCreated.class);
    }

    @Test
    void replayReproducesState() {
        UsageCharge original = charge();
        List<DomainEvent> stream = original.pullDomainEvents();

        UsageCharge rebuilt = UsageCharge.replay(stream);

        assertThat(rebuilt.id()).isEqualTo(original.id());
        assertThat(rebuilt.invoiceId()).isEqualTo(original.invoiceId());
        assertThat(rebuilt.totalPrice()).isEqualTo(original.totalPrice());
        assertThat(rebuilt.version()).isEqualTo(1L);
        assertThat(rebuilt.pullDomainEvents()).isEmpty();
    }
}
