package com.project.billing.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.DomainEvent;
import com.project.billing.domain.usage.event.UsageChargeCreated;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serialises billing {@link DomainEvent}s to/from the JSON stored in the event store. A stable
 * {@code eventType} tag is persisted with each payload so events re-materialise into the right
 * type on replay. Tags are decoupled from Java class names on purpose — classes may be refactored,
 * a stored tag never changes. Covers every event-sourced aggregate's events (invoice + usage).
 */
@Component
public class BillingEventSerializer {

    private final ObjectMapper objectMapper;

    private final Map<String, Class<? extends DomainEvent>> typeToClass = Map.of(
            "invoice.created", InvoiceEvent.Created.class,
            "invoice.activated", InvoiceEvent.Activated.class,
            "invoice.paid", InvoiceEvent.Paid.class,
            "invoice.failed", InvoiceEvent.Failed.class,
            "usage.charge-created", UsageChargeCreated.class
    );

    private final Map<Class<? extends DomainEvent>, String> classToType;

    public BillingEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.classToType = typeToClass.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    /** Stable persisted type tag for an event. */
    public String typeOf(DomainEvent event) {
        String type = classToType.get(event.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No stored type mapping for event " + event.getClass().getName());
        }
        return type;
    }

    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize billing event " + event.getClass().getName(), e);
        }
    }

    public DomainEvent deserialize(String eventType, String payload) {
        Class<? extends DomainEvent> target = typeToClass.get(eventType);
        if (target == null) {
            throw new IllegalStateException("Unknown billing event type '" + eventType + "' in event store");
        }
        try {
            return objectMapper.readValue(payload, target);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize billing event of type " + eventType, e);
        }
    }
}
