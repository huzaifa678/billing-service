package com.project.billing.application.usage.port.out;

import com.project.billing.domain.usage.UsageCharge;

/** Outbound port for persisting {@link UsageCharge} aggregates. */
public interface UsageChargeRepositoryPort {

    UsageCharge save(UsageCharge charge);
}
