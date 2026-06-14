package com.project.billing_service.service;

import com.project.billing_service.exceptions.InvalidSubscriptionStateException;
import com.project.billing_service.exceptions.InvoiceNotFoundException;
import com.project.billing_service.exceptions.PaymentFailedException;
import com.project.billing_service.model.dtos.InvoiceDto;
import com.project.billing_service.model.entities.InvoiceEntity;
import com.project.billing_service.model.entities.InvoiceStatus;
import com.project.billing_service.model.mapper.InvoiceMapper;
import com.project.billing_service.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.subscription.v1.GetSubscriptionResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String PAYMENT_SUCCEEDED = "succeeded";
    private static final String USAGE_METRIC_API_CALLS = "api_calls";
    private static final long USAGE_QUANTITY = 1L;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper mapper;
    private final BillingInterface paymentService; // strategy
    private final SubscriptionGateway subscriptionGateway;
    private final RateLimiterService rateLimiterService;
    private final UsageChargeService usageChargeService;

    public InvoiceEntity createInvoice(InvoiceDto dto) {

        GetSubscriptionResponse sub =
                subscriptionGateway.getSubscription(dto.getSubscriptionId().toString());

        if (!ACTIVE_STATUS.equals(sub.getStatus())) {
            throw new InvalidSubscriptionStateException(
                    "Cannot create invoice for inactive subscription"
            );
        }

        InvoiceEntity invoice = mapper.convertToEntity(dto);

        return invoiceRepository.save(invoice);
    }

    public List<InvoiceEntity> getInvoicesByAccount(UUID customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    public List<InvoiceEntity> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status);
    }

    public InvoiceEntity getInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new InvoiceNotFoundException(
                                "Invoice not found with id: " + invoiceId
                        )
                );
    }

    public String payInvoice(UUID invoiceId, String paymentMethodId) {

        rateLimiterService.checkPayInvoice(invoiceId);

        InvoiceEntity invoice = getInvoice(invoiceId);

        try {
            String status = paymentService.payInvoice(invoice, paymentMethodId);

            if (PAYMENT_SUCCEEDED.equals(status)) {
                recordUsageCharge(invoice);
            }

            return status;

        } catch (Exception e) {

            invoice.setStatus(InvoiceStatus.FAILED.name());
            invoiceRepository.save(invoice);

            throw new PaymentFailedException(
                    "Payment failed for invoice " + invoiceId + e.toString()
            );
        }
    }

    private void recordUsageCharge(InvoiceEntity invoice) {
        BigDecimal unitPrice =
                invoice.getAmount().divide(BigDecimal.valueOf(USAGE_QUANTITY));

        usageChargeService.createUsageCharge(
                invoice.getInvoiceId(),
                USAGE_METRIC_API_CALLS,
                USAGE_QUANTITY,
                unitPrice
        );
    }

    public List<GetSubscriptionResponse> getActiveSubscriptions(UUID userId) {
        return subscriptionGateway.getActiveSubscriptions(userId.toString());
    }

    @Transactional
    public void createInitialInvoice(UUID subscriptionId, UUID customerId) {

        invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(existing -> {
                    throw new InvalidSubscriptionStateException(
                            "Invoice already exists for subscription " + subscriptionId
                    );
                });

        InvoiceEntity invoice = InvoiceEntity.builder()
                .subscriptionId(subscriptionId)
                .customerId(customerId)
                .amount(new BigDecimal("29.99"))
                .currency("USD")
                .status(InvoiceStatus.ISSUED.name())
                .issuedAt(OffsetDateTime.now())
                .dueAt(OffsetDateTime.now().plusDays(7))
                .build();

        invoiceRepository.save(invoice);
    }
}
