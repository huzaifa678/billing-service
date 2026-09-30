package com.project.billing.application.usage.port.out;

import com.project.billing.domain.usage.UsageCharge;

/**
 * Outbound port for the usage-charge read-model projection. The event-sourced repository calls
 * this in the same transaction as the event append, keeping the {@code usage_charges} table
 * consistent with the event store.
 */
public interface UsageChargeProjectionPort {

    void project(UsageCharge charge);
}
