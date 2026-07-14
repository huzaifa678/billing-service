package com.project.billing.application.usage.port.in;

import com.project.billing.domain.usage.UsageCharge;

/** Inbound port: record a usage charge and publish its creation event. */
public interface RecordUsageChargeUseCase {

    UsageCharge record(RecordUsageChargeCommand command);
}
