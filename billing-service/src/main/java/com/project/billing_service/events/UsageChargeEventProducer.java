package com.project.billing_service.events;

import com.project.billing_service.avro.UsageChargeCreated;
import com.project.billing_service.model.entities.UsageChargeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Instant;


@Component
@RequiredArgsConstructor
public class UsageChargeEventProducer {
    private static final String TOPIC = "billing.usage-charge.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UsageChargeEntity entity) {

        UsageChargeCreated event = UsageChargeCreated.newBuilder()
                .setUsageChargeId(entity.getId())
                .setInvoiceId(entity.getInvoiceId())
                .setMetric(entity.getMetric())
                .setQuantity(entity.getQuantity())
                .setUnitPrice(entity.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                .setTotalPrice(entity.getTotalPrice().setScale(2, RoundingMode.HALF_UP))
                .setCreatedAt(Instant.ofEpochSecond(Instant.now().toEpochMilli()))
                .build();

        kafkaTemplate.send(
                TOPIC,
                entity.getInvoiceId().toString(),
                event
        );
    }
}
