package com.project.billing.adapter.out.messaging;

import com.project.billing.adapter.out.persistence.eventstore.BillingEventEntity;
import com.project.billing.adapter.out.persistence.eventstore.BillingEventJpaRepository;
import com.project.billing.adapter.out.persistence.eventstore.BillingEventSerializer;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.event.InvoiceEvent;
import com.project.billing.domain.shared.DomainEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingEventRelayTest {

    @Mock
    private BillingEventJpaRepository repository;
    @Mock
    private BillingEventSerializer serializer;
    @Mock
    private BillingEventAvroMapper avroMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private BillingEventRelay relay() {
        return new BillingEventRelay(repository, serializer, avroMapper, kafkaTemplate, 100);
    }

    private BillingEventEntity row(long globalSeq) {
        BillingEventEntity e = org.mockito.Mockito.mock(BillingEventEntity.class);
        lenient().when(e.getGlobalSeq()).thenReturn(globalSeq);
        lenient().when(e.getEventType()).thenReturn("invoice.paid");
        lenient().when(e.getPayload()).thenReturn("{}");
        lenient().when(e.getEventId()).thenReturn(UUID.randomUUID());
        return e;
    }

    @Test
    void relay_publishesUnpublishedThenMarksThem() {
        BillingEventRelay relay = relay();
        DomainEvent event = InvoiceEvent.Paid.of(InvoiceId.of(UUID.randomUUID()));
        // Build the mocked rows before stubbing the repository (each row() stubs its own mock, so
        // nesting them inside another when(...) would trip Mockito's UnfinishedStubbingException).
        List<BillingEventEntity> rows = List.of(row(1L), row(2L));
        when(repository.findByPublishedFalseOrderByGlobalSeqAsc(any(Pageable.class))).thenReturn(rows);
        when(serializer.deserialize(any(), any())).thenReturn(event);
        when(avroMapper.map(event)).thenReturn(
                new BillingEventAvroMapper.AvroMessage("billing.invoice.paid", "k", new Object()));
        doReturn(CompletableFuture.completedFuture(null)).when(kafkaTemplate).send(any(ProducerRecord.class));

        relay.relay();

        verify(kafkaTemplate, org.mockito.Mockito.times(2)).send(any(ProducerRecord.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> marked = ArgumentCaptor.forClass(List.class);
        verify(repository).markPublished(marked.capture(), any(Instant.class));
        assertThat(marked.getValue()).containsExactly(1L, 2L);
    }

    @Test
    void relay_nothingPending_doesNothing() {
        BillingEventRelay relay = relay();
        when(repository.findByPublishedFalseOrderByGlobalSeqAsc(any(Pageable.class))).thenReturn(List.of());

        relay.relay();

        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        verify(repository, never()).markPublished(any(), any());
    }

    @Test
    void relay_marksOnlyEventsPublishedBeforeAFailure() {
        BillingEventRelay relay = relay();
        DomainEvent event = InvoiceEvent.Paid.of(InvoiceId.of(UUID.randomUUID()));
        List<BillingEventEntity> rows = List.of(row(1L), row(2L));
        when(repository.findByPublishedFalseOrderByGlobalSeqAsc(any(Pageable.class))).thenReturn(rows);
        when(serializer.deserialize(any(), any())).thenReturn(event);
        when(avroMapper.map(event)).thenReturn(
                new BillingEventAvroMapper.AvroMessage("billing.invoice.paid", "k", new Object()));
        doReturn(CompletableFuture.completedFuture(null))
                .doReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")))
                .when(kafkaTemplate).send(any(ProducerRecord.class));

        relay.relay();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> marked = ArgumentCaptor.forClass(List.class);
        verify(repository).markPublished(marked.capture(), any(Instant.class));
        assertThat(marked.getValue()).containsExactly(1L);
    }
}
