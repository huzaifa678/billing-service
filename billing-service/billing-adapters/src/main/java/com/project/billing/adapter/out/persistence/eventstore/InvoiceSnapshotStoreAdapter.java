package com.project.billing.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.billing.application.invoice.port.out.InvoiceSnapshotStorePort;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed {@link InvoiceSnapshotStorePort}. Aggregate state is serialised to a self-contained
 * JSON document ({@link SnapshotState}) rather than mirroring the domain type, so refactors to
 * {@link Invoice} do not silently invalidate stored snapshots. {@link Money} is handled by
 * {@link MoneyJacksonModule}.
 */
@Component
@RequiredArgsConstructor
public class InvoiceSnapshotStoreAdapter implements InvoiceSnapshotStorePort {

    private final InvoiceSnapshotJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<Invoice> load(InvoiceId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public void save(Invoice invoice) {
        InvoiceSnapshotEntity entity = new InvoiceSnapshotEntity(
                invoice.id().value(),
                invoice.version(),
                write(toState(invoice)),
                Instant.now()
        );
        repository.save(entity);
    }

    private Invoice toDomain(InvoiceSnapshotEntity entity) {
        SnapshotState s = read(entity.getState());
        return Invoice.fromSnapshot(
                InvoiceId.of(s.invoiceId()),
                SubscriptionId.of(s.subscriptionId()),
                CustomerId.of(s.customerId()),
                s.amount(),
                InvoiceStatus.valueOf(s.status()),
                s.issuedAt(),
                s.dueAt(),
                entity.getVersion()
        );
    }

    private SnapshotState toState(Invoice invoice) {
        return new SnapshotState(
                invoice.id().value(),
                invoice.subscriptionId().value(),
                invoice.customerId().value(),
                invoice.amount(),
                invoice.status().name(),
                invoice.issuedAt(),
                invoice.dueAt()
        );
    }

    private String write(SnapshotState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize invoice snapshot " + state.invoiceId(), e);
        }
    }

    private SnapshotState read(String json) {
        try {
            return objectMapper.readValue(json, SnapshotState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize invoice snapshot", e);
        }
    }

    /** Self-contained snapshot document; independent of the domain type on purpose. */
    record SnapshotState(
            UUID invoiceId,
            UUID subscriptionId,
            UUID customerId,
            Money amount,
            String status,
            OffsetDateTime issuedAt,
            OffsetDateTime dueAt
    ) {
    }
}
