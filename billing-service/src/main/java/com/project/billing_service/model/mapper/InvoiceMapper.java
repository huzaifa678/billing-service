package com.project.billing_service.model.mapper;

import com.project.billing_service.model.dtos.InvoiceDto;
import com.project.billing_service.model.entities.InvoiceEntity;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper extends BaseMapper<InvoiceEntity, InvoiceDto> {

    @Override
    public InvoiceEntity convertToEntity(InvoiceDto dto, Object... args) {
        if (dto == null) {
            return null;
        }

        return InvoiceEntity.builder()
                .subscriptionId(dto.getSubscriptionId())
                .customerId(dto.getCustomerId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .issuedAt(dto.getIssuedAt())
                .dueAt(dto.getDueAt())
                .build();
    }

    @Override
    public InvoiceDto convertToDto(InvoiceEntity entity, Object... args) {
        if (entity == null) {
            return null;
        }

        return InvoiceDto.builder()
                .invoiceId(entity.getInvoiceId())
                .subscriptionId(entity.getSubscriptionId())
                .customerId(entity.getCustomerId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .issuedAt(entity.getIssuedAt())
                .dueAt(entity.getDueAt())
                .build();
    }
}
