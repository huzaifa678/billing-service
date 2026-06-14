package com.project.billing_service.service;

import billing.SubscriptionStatus;
import billing.SubscriptionUpdated;
import com.project.billing_service.model.entities.InvoiceStatus;
import com.project.billing_service.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class SubscriptionUpdateHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(SubscriptionUpdated event) {
        UUID subscriptionId = UUID.fromString(event.getSubscriptionId());
        SubscriptionStatus status = event.getStatus();

        switch (status) {
            case ACTIVE -> activateInvoice(subscriptionId);
            case CANCELED, EXPIRED -> failInvoice(subscriptionId);
            case TRIALING -> {
                // no invoice change while trialing
            }
        }
    }

    private void activateInvoice(UUID subscriptionId) {
        invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(invoice -> {
                    if (InvoiceStatus.DRAFT.name().equals(invoice.getStatus())) {
                        invoice.setStatus(InvoiceStatus.ISSUED.name());
                    }
                });
    }

    private void failInvoice(UUID subscriptionId) {
        invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(invoice -> {
                    invoice.setStatus(InvoiceStatus.FAILED.name());
                    invoiceRepository.save(invoice);
                });
    }
}
