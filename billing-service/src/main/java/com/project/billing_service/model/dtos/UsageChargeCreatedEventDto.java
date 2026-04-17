package com.project.billing_service.model.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record UsageChargeCreatedEventDto(
        UUID usageChargeId,
        UUID invoiceId,
        String metric,
        long quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {}
