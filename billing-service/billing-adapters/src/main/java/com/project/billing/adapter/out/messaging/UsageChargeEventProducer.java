package com.project.billing.adapter.out.messaging;

import com.project.billing.domain.usage.event.UsageChargeCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/** Publishes {@link UsageChargeCreated} domain events to Kafka as Avro records. */
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

        kafkaTemplate.send(
                TOPIC,
                event.invoiceId().value().toString(),
                avro
        );
    }
}
