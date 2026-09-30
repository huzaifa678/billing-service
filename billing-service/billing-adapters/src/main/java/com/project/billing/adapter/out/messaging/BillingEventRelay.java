package com.project.billing.adapter.out.messaging;

import com.project.billing.adapter.out.persistence.eventstore.BillingEventEntity;
import com.project.billing.adapter.out.persistence.eventstore.BillingEventJpaRepository;
import com.project.billing.adapter.out.persistence.eventstore.BillingEventSerializer;
import com.project.billing.domain.shared.DomainEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Transactional-outbox relay for billing events. Polls the shared event store for rows not yet
 * published, maps each to its Avro record (Schema Registry), sends it to Kafka keyed by the event's
 * partition key, and marks it published only after the broker acknowledges.
 *
 * <p>This decouples "durably stored" from "on the bus", removing the previous persist-then-publish
 * dual write. Delivery is at-least-once — consumers dedupe on the {@code eventId} header. Disabled
 * under the {@code test} profile so it does not poll a non-existent broker during integration tests.
 */
@Component
@Profile("!test")
public class BillingEventRelay {

    private static final Logger log = LoggerFactory.getLogger(BillingEventRelay.class);

    private final BillingEventJpaRepository repository;
    private final BillingEventSerializer serializer;
    private final BillingEventAvroMapper avroMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final int batchSize;

    public BillingEventRelay(
            BillingEventJpaRepository repository,
            BillingEventSerializer serializer,
            BillingEventAvroMapper avroMapper,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${billing.event-relay.batch-size:100}") int batchSize
    ) {
        this.repository = repository;
        this.serializer = serializer;
        this.avroMapper = avroMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${billing.event-relay.fixed-delay-ms:1000}")
    @Transactional
    public void relay() {
        List<BillingEventEntity> batch =
                repository.findByPublishedFalseOrderByGlobalSeqAsc(PageRequest.of(0, batchSize));
        if (batch.isEmpty()) {
            return;
        }

        List<Long> published = new ArrayList<>(batch.size());
        try {
            for (BillingEventEntity row : batch) {
                DomainEvent event = serializer.deserialize(row.getEventType(), row.getPayload());
                BillingEventAvroMapper.AvroMessage message = avroMapper.map(event);

                ProducerRecord<String, Object> record =
                        new ProducerRecord<>(message.topic(), message.key(), message.value());
                record.headers().add(new RecordHeader("eventType",
                        row.getEventType().getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("eventId",
                        row.getEventId().toString().getBytes(StandardCharsets.UTF_8)));

                // Block for the broker ack so a row is marked published only once it is on the bus.
                kafkaTemplate.send(record).get();
                published.add(row.getGlobalSeq());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Billing event relay interrupted after {} of {} events; rest retried next poll",
                    published.size(), batch.size());
        } catch (Exception e) {
            // Stop at the first failure to preserve per-aggregate ordering; retry from here next poll.
            log.error("Billing event relay failed after {} of {} events; rest retried next poll",
                    published.size(), batch.size(), e);
        }

        if (!published.isEmpty()) {
            repository.markPublished(published, Instant.now());
        }
    }
}
