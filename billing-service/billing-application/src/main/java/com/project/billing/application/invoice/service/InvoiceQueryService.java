package com.project.billing.application.invoice.service;

import com.project.billing.application.invoice.port.in.InvoiceQueries;
import com.project.billing.application.invoice.port.in.SubscriptionQueries;
import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.application.invoice.port.out.SubscriptionGatewayPort;
import com.project.billing.domain.exception.InvoiceNotFoundException;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.subscription.SubscriptionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Query side (read model) for invoices and subscription data. Reads flow through
 * the same repository/gateway ports and the single database — light CQRS, no
 * separate read store.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceQueryService implements InvoiceQueries, SubscriptionQueries {

    private final InvoiceRepositoryPort invoiceRepository;
    private final SubscriptionGatewayPort subscriptionGateway;

    @Override
    public Invoice getById(UUID invoiceId) {
        return invoiceRepository.findById(InvoiceId.of(invoiceId))
                .orElseThrow(() -> new InvoiceNotFoundException(
                        "Invoice not found with id: " + invoiceId
                ));
    }

    @Override
    public List<Invoice> byCustomer(UUID customerId) {
        return invoiceRepository.findByCustomerId(CustomerId.of(customerId));
    }

    @Override
    public List<Invoice> byStatus(String status) {
        return invoiceRepository.findByStatus(InvoiceStatus.valueOf(status));
    }

    @Override
    public List<SubscriptionSnapshot> activeByUser(UUID userId) {
        return subscriptionGateway.getActiveSubscriptions(CustomerId.of(userId));
    }
}
