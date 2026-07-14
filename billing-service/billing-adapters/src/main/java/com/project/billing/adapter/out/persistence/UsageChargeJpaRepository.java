package com.project.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data repository for {@link UsageChargeJpaEntity}. */
public interface UsageChargeJpaRepository extends JpaRepository<UsageChargeJpaEntity, UUID> {
}
