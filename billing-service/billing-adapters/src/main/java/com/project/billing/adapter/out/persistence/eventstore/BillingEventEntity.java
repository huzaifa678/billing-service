package com.project.billing.adapter.out.persistence.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single row in the shared append-only billing event store. Serves every event-sourced
 * aggregate (invoices, usage charges), discriminated by {@code aggregate_type}. The unique
 * {@code (aggregate_type, aggregate_id, sequence)} constraint enforces optimistic concurrency on
 * append; {@code global_seq} gives a store-wide order the outbox relay walks to publish to Kafka.
 */
@Entity
@Table(
        name = "billing_event_store",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_billing_event_stream_sequence",
                        columnNames = {"aggregate_type", "aggregate_id", "sequence"}),
                @UniqueConstraint(name = "uq_billing_event_event_id", columnNames = {"event_id"})
        }
)
public class BillingEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "global_seq")
    private Long globalSeq;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 32)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "sequence", nullable = false)
    private long sequence;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected BillingEventEntity() {
        // for JPA
    }

    private BillingEventEntity(Builder b) {
        this.eventId = b.eventId;
        this.aggregateType = b.aggregateType;
        this.aggregateId = b.aggregateId;
        this.sequence = b.sequence;
        this.eventType = b.eventType;
        this.payload = b.payload;
        this.occurredAt = b.occurredAt;
        this.published = b.published;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getGlobalSeq() {
        return globalSeq;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public long getSequence() {
        return sequence;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public static final class Builder {
        private UUID eventId;
        private String aggregateType;
        private UUID aggregateId;
        private long sequence;
        private String eventType;
        private String payload;
        private Instant occurredAt;
        private boolean published;

        public Builder eventId(UUID v) { this.eventId = v; return this; }
        public Builder aggregateType(String v) { this.aggregateType = v; return this; }
        public Builder aggregateId(UUID v) { this.aggregateId = v; return this; }
        public Builder sequence(long v) { this.sequence = v; return this; }
        public Builder eventType(String v) { this.eventType = v; return this; }
        public Builder payload(String v) { this.payload = v; return this; }
        public Builder occurredAt(Instant v) { this.occurredAt = v; return this; }
        public Builder published(boolean v) { this.published = v; return this; }

        public BillingEventEntity build() {
            return new BillingEventEntity(this);
        }
    }
}
