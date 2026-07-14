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
import java.util.UUID;

/**
 * JPA persistence model for usage charges. Column names match the previous
 * {@code UsageChargeEntity} (derived by the default naming strategy) to keep the
 * existing schema unchanged.
 */
@Entity
@Table(name = "usage_charges")
public class UsageChargeJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private String metric;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Transient
    private boolean isNew;

    protected UsageChargeJpaEntity() {
        // for JPA
    }

    public UsageChargeJpaEntity(
            UUID id,
            UUID invoiceId,
            String metric,
            long quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            boolean isNew
    ) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.metric = metric;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.isNew = isNew;
    }

    @Override
    public UUID getId() {
        return id;
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

    public String getMetric() {
        return metric;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
}
