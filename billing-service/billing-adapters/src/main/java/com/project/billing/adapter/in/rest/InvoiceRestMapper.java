package com.project.billing.adapter.in.rest;

import com.project.billing.adapter.in.rest.dto.InvoiceRequest;
import com.project.billing.adapter.in.rest.dto.InvoiceResponse;
import com.project.billing.adapter.in.rest.dto.SubscriptionResponse;
import com.project.billing.application.invoice.port.in.CreateInvoiceCommand;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.subscription.SubscriptionSnapshot;
import org.springframework.stereotype.Component;

/** Maps between REST DTOs and application commands / domain models. */
@Component
public class InvoiceRestMapper {

    public CreateInvoiceCommand toCommand(InvoiceRequest request) {
        return new CreateInvoiceCommand(
                request.subscriptionId(),
                request.customerId(),
                request.amount(),
                request.currency(),
                request.status(),
                request.issuedAt(),
                request.dueAt()
        );
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.id().value(),
                invoice.subscriptionId().value(),
                invoice.customerId().value(),
                invoice.amount().amount(),
                invoice.amount().currency(),
                invoice.status().name(),
                invoice.issuedAt(),
                invoice.dueAt()
        );
    }

    public SubscriptionResponse toResponse(SubscriptionSnapshot snapshot) {
        return new SubscriptionResponse(
                snapshot.subscriptionId(),
                snapshot.userId(),
                snapshot.planId(),
                snapshot.status(),
                snapshot.currentPeriodStart(),
                snapshot.currentPeriodEnd(),
                snapshot.cancelAtPeriodEnd(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }
}
