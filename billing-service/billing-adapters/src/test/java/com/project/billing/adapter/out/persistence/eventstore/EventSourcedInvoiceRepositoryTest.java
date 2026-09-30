package com.project.billing.adapter.out.persistence.eventstore;

import com.project.billing.application.invoice.port.out.InvoiceProjectionPort;
import com.project.billing.application.invoice.port.out.InvoiceReadModelPort;
import com.project.billing.application.invoice.port.out.InvoiceSnapshotStorePort;
import com.project.billing.application.shared.port.out.EventStorePort;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSourcedInvoiceRepositoryTest {

    @Mock
    private EventStorePort eventStore;
    @Mock
    private InvoiceSnapshotStorePort snapshotStore;
    @Mock
    private InvoiceProjectionPort projection;
    @Mock
    private InvoiceReadModelPort readModel;

    private EventSourcedInvoiceRepository repository() {
        return new EventSourcedInvoiceRepository(eventStore, snapshotStore, projection, readModel, 2);
    }

    private static Invoice draft() {
        return Invoice.create(
                SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID()),
                Money.of("100", "USD"), InvoiceStatus.DRAFT,
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(7));
    }

    @Test
    void save_appendsAtExpectedVersionAndProjects() {
        EventSourcedInvoiceRepository repo = repository();
        Invoice invoice = draft(); // 1 pending event (Created, v1)

        repo.save(invoice);

        ArgumentCaptor<Long> expected = ArgumentCaptor.forClass(Long.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(eventStore).append(eq("invoice"), eq(invoice.id().value()), expected.capture(), events.capture());
        assertThat(expected.getValue()).isEqualTo(0L);
        assertThat(events.getValue()).hasSize(1);
        verify(projection).project(invoice);
        verify(snapshotStore, never()).save(any()); // below interval 2
        assertThat(invoice.pullDomainEvents()).isEmpty(); // drained by save
    }

    @Test
    void save_takesSnapshotWhenCrossingInterval() {
        EventSourcedInvoiceRepository repo = repository();
        Invoice invoice = draft(); // Created v1
        invoice.markPaid();        // Paid v2 -> crosses interval 2

        repo.save(invoice);

        verify(eventStore).append(eq("invoice"), eq(invoice.id().value()), eq(0L), any());
        verify(snapshotStore).save(invoice);
    }

    @Test
    void save_noPending_isNoOp() {
        EventSourcedInvoiceRepository repo = repository();
        Invoice invoice = draft();
        invoice.pullDomainEvents(); // drain

        repo.save(invoice);

        verify(eventStore, never()).append(any(), any(), anyLong(), any());
        verify(projection, never()).project(any());
    }

    @Test
    void findByIdForUpdate_locksProjectionThenReplays() {
        EventSourcedInvoiceRepository repo = repository();
        Invoice source = draft();
        List<DomainEvent> stream = source.pullDomainEvents();
        InvoiceId id = source.id();

        Invoice projected = Invoice.reconstitute(id, source.subscriptionId(), source.customerId(),
                source.amount(), InvoiceStatus.DRAFT, source.issuedAt(), source.dueAt());
        when(readModel.findByIdForUpdate(id)).thenReturn(Optional.of(projected));
        when(snapshotStore.load(id)).thenReturn(Optional.empty());
        when(eventStore.loadAfter("invoice", id.value(), 0L)).thenReturn(stream);

        Invoice loaded = repo.findByIdForUpdate(id).orElseThrow();

        verify(readModel).findByIdForUpdate(id); // pessimistic lock acquired
        assertThat(loaded.version()).isEqualTo(1L);
        assertThat(loaded.id()).isEqualTo(id);
    }

    @Test
    void findByIdForUpdate_absent_returnsEmptyWithoutStore() {
        EventSourcedInvoiceRepository repo = repository();
        InvoiceId id = InvoiceId.of(UUID.randomUUID());
        when(readModel.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThat(repo.findByIdForUpdate(id)).isEmpty();
        verify(eventStore, never()).loadAfter(any(), any(), anyLong());
    }

    @Test
    void findBySubscriptionId_replaysFullStream() {
        EventSourcedInvoiceRepository repo = repository();
        Invoice source = draft();
        source.markPaid();
        List<DomainEvent> stream = source.pullDomainEvents(); // Created + Paid
        InvoiceId id = source.id();
        SubscriptionId subscriptionId = source.subscriptionId();

        Invoice projected = Invoice.reconstitute(id, subscriptionId, source.customerId(),
                source.amount(), InvoiceStatus.PAID, source.issuedAt(), source.dueAt());
        when(readModel.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(projected));
        when(snapshotStore.load(id)).thenReturn(Optional.empty());
        when(eventStore.loadAfter("invoice", id.value(), 0L)).thenReturn(stream);

        Invoice loaded = repo.findBySubscriptionId(subscriptionId).orElseThrow();

        assertThat(loaded.version()).isEqualTo(2L);
        assertThat(loaded.status()).isEqualTo(InvoiceStatus.PAID);
    }
}
