package com.project.billing.domain.invoice.event;

import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Sealed catalogue of the facts an {@link com.project.billing.domain.invoice.Invoice} emits.
 * These events are the aggregate's source of truth: current state is rebuilt by folding them in
 * order. Each carries its {@link #invoiceId()} for routing/consumers; the event store assigns the
 * per-aggregate sequence and a unique event id at append time (they are not part of the fact).
 *
 * <p>Events are immutable and pure — value objects and primitives only. Because the store is
 * append-only, existing shapes must not change; evolve by adding new event types.
 */
public sealed interface InvoiceEvent extends DomainEvent {

    /** Identity of the invoice this event belongs to. */
    InvoiceId invoiceId();

    /** An invoice was created (draft or issued). Always the first event in the stream. */
    record Created(
            InvoiceId invoiceId,
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt,
            Instant occurredOn
    ) implements InvoiceEvent {
        public static Created of(
                InvoiceId invoiceId,
                SubscriptionId subscriptionId,
                CustomerId customerId,
                Money amount,
                InvoiceStatus status,
                OffsetDateTime issuedAt,
                OffsetDateTime dueAt
        ) {
            return new Created(invoiceId, subscriptionId, customerId, amount, status, issuedAt, dueAt, Instant.now());
        }
    }

    /** A draft invoice was promoted to issued. */
    record Activated(InvoiceId invoiceId, Instant occurredOn) implements InvoiceEvent {
        public static Activated of(InvoiceId invoiceId) {
            return new Activated(invoiceId, Instant.now());
        }
    }

    /** The invoice was paid successfully. */
    record Paid(InvoiceId invoiceId, Instant occurredOn) implements InvoiceEvent {
        public static Paid of(InvoiceId invoiceId) {
            return new Paid(invoiceId, Instant.now());
        }
    }

    /** The invoice failed (payment failure, or subscription cancellation/expiry). */
    record Failed(InvoiceId invoiceId, Instant occurredOn) implements InvoiceEvent {
        public static Failed of(InvoiceId invoiceId) {
            return new Failed(invoiceId, Instant.now());
        }
    }
}
