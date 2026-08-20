package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.usage.event.UsageChargeCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/** Publishes {@link UsageChargeCreated} domain events to Kafka as Avro records. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageChargeEventProducer {

    private static final String TOPIC = "billing.usage-charge.created";
    private static final int MONEY_SCALE = 2;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UsageChargeCreated event) {
        com.project.billing_service.avro.UsageChargeCreated avro =
                com.project.billing_service.avro.UsageChargeCreated.newBuilder()
                        .setUsageChargeId(event.usageChargeId().value())
                        .setInvoiceId(event.invoiceId().value())
                        .setMetric(event.metric().value())
                        .setQuantity(event.quantity())
                        .setUnitPrice(event.unitPrice().amount().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                        .setTotalPrice(event.totalPrice().amount().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                        .setCreatedAt(event.occurredOn())
                        .build();

        String key = event.invoiceId().value().toString();
        kafkaTemplate.send(TOPIC, key, avro)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // The message never left the app, so there is nothing to dead-letter;
                        // surface the failure to Loki via the OTLP appender for alerting/replay.
                        log.error("Failed to publish UsageChargeCreated to {} for invoice {}", TOPIC, key, ex);
                    }
                });
    }
}
