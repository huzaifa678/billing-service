package com.project.billing_service.model.dtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceDto {
    private UUID invoiceId;
    private UUID subscriptionId;
    private UUID customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private OffsetDateTime issuedAt;
    private OffsetDateTime dueAt;
}
