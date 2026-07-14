package com.project.billing.domain.shared;

import java.time.Instant;

/**
 * Marker interface for domain events raised by aggregates. Events are pure —
 * they carry value objects and primitives only, never framework or persistence
 * types — and are published by the application layer through an outbound port.
 */
public interface DomainEvent {

    Instant occurredOn();
}
