package com.project.billing.adapter.out.persistence.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A materialised {@link com.project.billing.domain.invoice.Invoice} state at a known version.
 * One row per invoice (upserted); loading it and replaying only later events bounds replay cost.
 * Derived data — safe to truncate and rebuild from the event store.
 */
@Entity
@Table(name = "invoice_snapshot")
public class InvoiceSnapshotEntity {

    @Id
    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column(name = "version", nullable = false)
    private long version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state", nullable = false, columnDefinition = "jsonb")
    private String state;

    @Column(name = "taken_at", nullable = false)
    private Instant takenAt;

    protected InvoiceSnapshotEntity() {
        // for JPA
    }

    public InvoiceSnapshotEntity(UUID aggregateId, long version, String state, Instant takenAt) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.state = state;
        this.takenAt = takenAt;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public long getVersion() {
        return version;
    }

    public String getState() {
        return state;
    }

    public Instant getTakenAt() {
        return takenAt;
    }
}
