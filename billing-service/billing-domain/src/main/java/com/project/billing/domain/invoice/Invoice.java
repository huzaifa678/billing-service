package com.project.billing.domain.invoice;

import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.AbstractAggregateRoot;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice aggregate root, modelled as an event-sourced aggregate. Lifecycle transitions
 * (draft → issued → paid/failed) never mutate fields directly: behaviour validates invariants,
 * then raises an {@link InvoiceEvent} that is both folded into state and recorded for the event
 * store. Current state is therefore the fold of the event stream, rebuildable via
 * {@link #replay(List)} (optionally onto a snapshot with {@link #fromSnapshot} + {@link #replayAll}).
 *
 * <p>{@link #version()} (from the base) is the number of applied events and the basis for the
 * event store's optimistic concurrency on append.
 */
public class Invoice extends AbstractAggregateRoot {

    private static final Money INITIAL_AMOUNT = Money.of("29.99", "USD");
    private static final int DEFAULT_DUE_DAYS = 7;

    private final InvoiceId id;
    private SubscriptionId subscriptionId;
    private CustomerId customerId;
    private Money amount;
    private InvoiceStatus status;
    private OffsetDateTime issuedAt;
    private OffsetDateTime dueAt;

    private Invoice(InvoiceId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /** Create a new invoice from explicit values (REST create flow), raising {@link InvoiceEvent.Created}. */
    public static Invoice create(
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt
    ) {
        InvoiceId id = InvoiceId.of(UUID.randomUUID());
        Invoice invoice = new Invoice(id);
        invoice.applyChange(InvoiceEvent.Created.of(id, subscriptionId, customerId, amount, status, issuedAt, dueAt));
        return invoice;
    }

    /**
     * Create the default initial invoice raised when a subscription is created
     * (29.99 USD, issued immediately, due in 7 days).
     */
    public static Invoice issueInitial(SubscriptionId subscriptionId, CustomerId customerId) {
        OffsetDateTime now = OffsetDateTime.now();
        return create(subscriptionId, customerId, INITIAL_AMOUNT, InvoiceStatus.ISSUED, now, now.plusDays(DEFAULT_DUE_DAYS));
    }

    /**
     * Rebuild an invoice from an already-materialised read-model row (projection). Raises no
     * events; the returned aggregate is for reads only, not to be mutated and saved.
     */
    public static Invoice reconstitute(
            InvoiceId id,
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt
    ) {
        return fromSnapshot(id, subscriptionId, customerId, amount, status, issuedAt, dueAt, 0L);
    }

    /** Rebuild an invoice from a stored snapshot at a known {@code version}. */
    public static Invoice fromSnapshot(
            InvoiceId id,
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt,
            long version
    ) {
        Invoice invoice = new Invoice(id);
        invoice.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId");
        invoice.customerId = Objects.requireNonNull(customerId, "customerId");
        invoice.amount = Objects.requireNonNull(amount, "amount");
        invoice.status = Objects.requireNonNull(status, "status");
        invoice.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        invoice.dueAt = Objects.requireNonNull(dueAt, "dueAt");
        invoice.restoreVersion(version);
        return invoice;
    }

    /** Rebuild an invoice from its full event stream (first event must be {@link InvoiceEvent.Created}). */
    public static Invoice replay(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Cannot replay an invoice from an empty event stream.");
        }
        if (!(events.get(0) instanceof InvoiceEvent.Created created)) {
            throw new IllegalArgumentException("The first invoice event must be Created.");
        }
        Invoice invoice = new Invoice(created.invoiceId());
        invoice.replayAll(events);
        return invoice;
    }

    /** Apply a tail of events (e.g. those after a snapshot) without recording them. */
    public void replayAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            apply((InvoiceEvent) event);
        }
    }

    /** Promote a draft invoice to issued; no-op for any other status. */
    public void activate() {
        if (status == InvoiceStatus.DRAFT) {
            applyChange(InvoiceEvent.Activated.of(id));
        }
    }

    /** Mark the invoice as successfully paid. */
    public void markPaid() {
        applyChange(InvoiceEvent.Paid.of(id));
    }

    /** Mark the invoice as failed (payment failure or subscription cancellation/expiry). */
    public void markFailed() {
        applyChange(InvoiceEvent.Failed.of(id));
    }

    private void applyChange(InvoiceEvent event) {
        apply(event);
        registerEvent(event);
    }

    private void apply(InvoiceEvent event) {
        switch (event) {
            case InvoiceEvent.Created e -> applyCreated(e);
            case InvoiceEvent.Activated e -> this.status = InvoiceStatus.ISSUED;
            case InvoiceEvent.Paid e -> this.status = InvoiceStatus.PAID;
            case InvoiceEvent.Failed e -> this.status = InvoiceStatus.FAILED;
        }
        markApplied();
    }

    private void applyCreated(InvoiceEvent.Created e) {
        this.subscriptionId = e.subscriptionId();
        this.customerId = e.customerId();
        this.amount = e.amount();
        this.status = e.status();
        this.issuedAt = e.issuedAt();
        this.dueAt = e.dueAt();
    }

    public InvoiceId id() {
        return id;
    }

    public SubscriptionId subscriptionId() {
        return subscriptionId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Money amount() {
        return amount;
    }

    public InvoiceStatus status() {
        return status;
    }

    public OffsetDateTime issuedAt() {
        return issuedAt;
    }

    public OffsetDateTime dueAt() {
        return dueAt;
    }
}
