package com.project.billing.domain.invoice;

import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies Invoice behaves as a correct event-sourced aggregate. */
class InvoiceEventSourcingTest {

    private static Invoice draft() {
        return Invoice.create(
                SubscriptionId.of(UUID.randomUUID()),
                CustomerId.of(UUID.randomUUID()),
                Money.of("100", "USD"),
                InvoiceStatus.DRAFT,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(7));
    }

    @Test
    void mutationsRaiseEventsAndAdvanceVersion() {
        Invoice invoice = draft();          // Created, v1
        invoice.activate();                 // Activated, v2
        invoice.markPaid();                 // Paid, v3

        List<DomainEvent> events = invoice.pullDomainEvents();
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isInstanceOf(InvoiceEvent.Created.class);
        assertThat(events.get(1)).isInstanceOf(InvoiceEvent.Activated.class);
        assertThat(events.get(2)).isInstanceOf(InvoiceEvent.Paid.class);
        assertThat(invoice.version()).isEqualTo(3L);
        // Drained: a second pull returns nothing.
        assertThat(invoice.pullDomainEvents()).isEmpty();
    }

    @Test
    void activateIsANoOpUnlessDraft() {
        Invoice issued = Invoice.issueInitial(
                SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID())); // ISSUED, v1
        issued.pullDomainEvents();
        issued.activate(); // already ISSUED -> no event
        assertThat(issued.pullDomainEvents()).isEmpty();
        assertThat(issued.version()).isEqualTo(1L);
    }

    @Test
    void replayReproducesState() {
        Invoice original = draft();
        original.activate();
        original.markPaid();
        List<DomainEvent> stream = original.pullDomainEvents();

        Invoice rebuilt = Invoice.replay(stream);

        assertThat(rebuilt.id()).isEqualTo(original.id());
        assertThat(rebuilt.subscriptionId()).isEqualTo(original.subscriptionId());
        assertThat(rebuilt.amount()).isEqualTo(original.amount());
        assertThat(rebuilt.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(rebuilt.version()).isEqualTo(3L);
        assertThat(rebuilt.pullDomainEvents()).isEmpty();
    }

    @Test
    void snapshotPlusTailEqualsFullReplay() {
        Invoice original = draft();
        original.activate();
        original.markPaid();
        List<DomainEvent> stream = original.pullDomainEvents(); // v1..v3

        Invoice atV2 = Invoice.replay(stream.subList(0, 2)); // Created + Activated
        assertThat(atV2.version()).isEqualTo(2L);
        assertThat(atV2.status()).isEqualTo(InvoiceStatus.ISSUED);

        atV2.replayAll(stream.subList(2, 3)); // + Paid
        assertThat(atV2.version()).isEqualTo(3L);
        assertThat(atV2.status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void continuingAfterReplayNumbersFromCurrentVersion() {
        Invoice original = draft();
        Invoice rebuilt = Invoice.replay(original.pullDomainEvents()); // v1

        rebuilt.markPaid();
        assertThat(rebuilt.version()).isEqualTo(2L);
        assertThat(rebuilt.pullDomainEvents()).hasSize(1);
    }
}
