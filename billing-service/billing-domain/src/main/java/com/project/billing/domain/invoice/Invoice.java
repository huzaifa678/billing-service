package com.project.billing.domain.invoice;

import com.project.billing.domain.shared.AbstractAggregateRoot;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice aggregate root. Owns its lifecycle transitions (draft → issued → paid/failed)
 * and the invariants around them, replacing the previously anemic {@code InvoiceEntity}
 * whose status was mutated from several services. Identity is assigned by the domain at
 * creation time; {@link #isNew()} lets the persistence adapter choose insert vs. update.
 */
public class Invoice extends AbstractAggregateRoot {

    private static final Money INITIAL_AMOUNT = Money.of("29.99", "USD");
    private static final int DEFAULT_DUE_DAYS = 7;

    private final InvoiceId id;
    private final SubscriptionId subscriptionId;
    private final CustomerId customerId;
    private final Money amount;
    private InvoiceStatus status;
    private final OffsetDateTime issuedAt;
    private final OffsetDateTime dueAt;
    private final transient boolean isNew;

    private Invoice(
            InvoiceId id,
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt,
            boolean isNew
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.status = Objects.requireNonNull(status, "status");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt");
        this.isNew = isNew;
    }

    /** Create a new, not-yet-persisted invoice from explicit values (REST create flow). */
    public static Invoice create(
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt
    ) {
        return new Invoice(
                InvoiceId.of(UUID.randomUUID()),
                subscriptionId, customerId, amount, status, issuedAt, dueAt, true
        );
    }

    /**
     * Create the default initial invoice raised when a subscription is created
     * (29.99 USD, issued immediately, due in 7 days).
     */
    public static Invoice issueInitial(SubscriptionId subscriptionId, CustomerId customerId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Invoice(
                InvoiceId.of(UUID.randomUUID()),
                subscriptionId,
                customerId,
                INITIAL_AMOUNT,
                InvoiceStatus.ISSUED,
                now,
                now.plusDays(DEFAULT_DUE_DAYS),
                true
        );
    }

    /** Rebuild an invoice from persisted state (used by the persistence adapter). */
    public static Invoice reconstitute(
            InvoiceId id,
            SubscriptionId subscriptionId,
            CustomerId customerId,
            Money amount,
            InvoiceStatus status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt
    ) {
        return new Invoice(id, subscriptionId, customerId, amount, status, issuedAt, dueAt, false);
    }

    /** Promote a draft invoice to issued; no-op for any other status. */
    public void activate() {
        if (status == InvoiceStatus.DRAFT) {
            status = InvoiceStatus.ISSUED;
        }
    }

    /** Mark the invoice as successfully paid. */
    public void markPaid() {
        this.status = InvoiceStatus.PAID;
    }

    /** Mark the invoice as failed (payment failure or subscription cancellation/expiry). */
    public void markFailed() {
        this.status = InvoiceStatus.FAILED;
    }

    public boolean isNew() {
        return isNew;
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
