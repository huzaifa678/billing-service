package com.project.billing.adapter.out.persistence.eventstore;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Spring Data repository over the shared {@code billing_event_store} table. */
public interface BillingEventJpaRepository extends JpaRepository<BillingEventEntity, Long> {

    List<BillingEventEntity> findByAggregateTypeAndAggregateIdAndSequenceGreaterThanOrderBySequenceAsc(
            String aggregateType, UUID aggregateId, long afterSequence);

    /** Unpublished events across all aggregates in global order — the outbox relay's work queue. */
    List<BillingEventEntity> findByPublishedFalseOrderByGlobalSeqAsc(Pageable pageable);

    @Modifying
    @Query("update BillingEventEntity e set e.published = true, e.publishedAt = :publishedAt "
            + "where e.globalSeq in :globalSeqs")
    int markPublished(@Param("globalSeqs") List<Long> globalSeqs, @Param("publishedAt") Instant publishedAt);
}
