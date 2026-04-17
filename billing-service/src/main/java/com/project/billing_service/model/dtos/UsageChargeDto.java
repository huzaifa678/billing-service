package com.project.billing_service.model.dtos;

import java.math.BigDecimal;

public record UsageChargeDto(
    String metric,
    long quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}
