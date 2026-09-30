package com.project.billing.adapter.out.persistence.eventstore;

import com.project.billing.application.invoice.port.out.InvoiceProjectionPort;
import com.project.billing.application.invoice.port.out.InvoiceReadModelPort;
import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.application.invoice.port.out.InvoiceSnapshotStorePort;
import com.project.billing.application.shared.port.out.EventStorePort;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.SubscriptionId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Event-sourced {@link InvoiceRepositoryPort} — the command-side write repository and the
 * {@code @Primary} bean the invoice command service injects.
 *
 * <p>{@code save} drains the aggregate's pending events, appends them to the event store at the
 * expected version (optimistic concurrency), updates the read-model projection, and periodically
 * snapshots — all in the caller's transaction, so the event store and projection commit atomically.
 * {@code findByIdForUpdate} preserves the pre-event-sourcing double-charge protection: it takes the
 * pessimistic lock on the always-present projection row, then rebuilds the aggregate from the store.
 *
 * <p>Kafka publishing is intentionally out of band ({@link com.project.billing.adapter.out.messaging.BillingEventRelay}),
 * removing the persist-then-publish dual write.
 */
@Component
@Primary
public class EventSourcedInvoiceRepository implements InvoiceRepositoryPort {

    static final String AGGREGATE_TYPE = "invoice";

    private final EventStorePort eventStore;
    private final InvoiceSnapshotStorePort snapshotStore;
    private final InvoiceProjectionPort projection;
    private final InvoiceReadModelPort readModel;
    private final int snapshotInterval;

    public EventSourcedInvoiceRepository(
            EventStorePort eventStore,
            InvoiceSnapshotStorePort snapshotStore,
            InvoiceProjectionPort projection,
            InvoiceReadModelPort readModel,
            @Value("${billing.invoice.snapshot-interval:50}") int snapshotInterval
    ) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.projection = projection;
        this.readModel = readModel;
        this.snapshotInterval = snapshotInterval;
    }

    @Override
    public Invoice save(Invoice invoice) {
        List<DomainEvent> pending = invoice.pullDomainEvents();
        if (pending.isEmpty()) {
            return invoice;
        }

        long newVersion = invoice.version();
        long expectedVersion = newVersion - pending.size();

        eventStore.append(AGGREGATE_TYPE, invoice.id().value(), expectedVersion, pending);
        projection.project(invoice);

        if (crossedSnapshotBoundary(expectedVersion, newVersion)) {
            snapshotStore.save(invoice);
        }
        return invoice;
    }

    @Override
    public Optional<Invoice> findByIdForUpdate(InvoiceId id) {
        // Acquire the pessimistic lock on the projection row (serializes concurrent pays), then
        // rebuild the authoritative aggregate from the event store.
        return readModel.findByIdForUpdate(id).flatMap(locked -> loadFromStore(locked.id()));
    }

    @Override
    public Optional<Invoice> findBySubscriptionId(SubscriptionId subscriptionId) {
        return readModel.findBySubscriptionId(subscriptionId).flatMap(projected -> loadFromStore(projected.id()));
    }

    /** Rebuild the aggregate from its latest snapshot plus the events after it (or the full stream). */
    private Optional<Invoice> loadFromStore(InvoiceId id) {
        Optional<Invoice> snapshot = snapshotStore.load(id);
        if (snapshot.isPresent()) {
            Invoice invoice = snapshot.get();
            List<DomainEvent> tail = eventStore.loadAfter(AGGREGATE_TYPE, id.value(), invoice.version());
            invoice.replayAll(tail);
            return Optional.of(invoice);
        }

        List<DomainEvent> events = eventStore.loadAfter(AGGREGATE_TYPE, id.value(), 0L);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Invoice.replay(events));
    }

    private boolean crossedSnapshotBoundary(long expectedVersion, long newVersion) {
        if (snapshotInterval <= 0) {
            return false;
        }
        return Math.floorDiv(newVersion, snapshotInterval) > Math.floorDiv(expectedVersion, snapshotInterval);
    }
}
