package com.project.billing.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA persistence model for invoices. This is where JPA leaks are confined — the
 * domain {@link com.project.billing.domain.invoice.Invoice} stays framework-free.
 * Implements {@link Persistable} because identity is assigned by the domain, so
 * Hibernate cannot infer insert-vs-update from a null id.
 */
@Entity
@Table(name = "invoices")
public class InvoiceJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Transient
    private boolean isNew;

    protected InvoiceJpaEntity() {
        // for JPA
    }

    public InvoiceJpaEntity(
            UUID invoiceId,
            UUID subscriptionId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            String status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt,
            boolean isNew
    ) {
        this.invoiceId = invoiceId;
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.issuedAt = issuedAt;
        this.dueAt = dueAt;
        this.isNew = isNew;
    }

    @Override
    public UUID getId() {
        return invoiceId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }
}
