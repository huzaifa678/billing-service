package com.project.billing_service.service;

import billing.SubscriptionStatus;
import billing.SubscriptionUpdated;
import com.project.billing_service.client.SubscriptionGrpcClient;
import com.project.billing_service.exceptions.InvalidSubscriptionStateException;
import com.project.billing_service.exceptions.InvoiceNotFoundException;
import com.project.billing_service.exceptions.PaymentFailedException;
import com.project.billing_service.exceptions.SubscriptionServiceUnavailableException;
import com.project.billing_service.model.dtos.InvoiceDto;
import com.project.billing_service.model.entities.InvoiceEntity;
import com.project.billing_service.model.entities.InvoiceStatus;
import com.project.billing_service.model.mapper.InvoiceMapper;
import com.project.billing_service.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import subscription.Subscription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper mapper;
    private final BillingInterface paymentService; // strategy
    private final SubscriptionGrpcClient subscriptionGrpcClient;
    private final RateLimiterService rateLimiterService;
    private final UsageChargeService usageChargeService;


    public InvoiceEntity createInvoice(InvoiceDto dto) {

        Subscription.SubscriptionResponse sub;
        try {
            sub = subscriptionGrpcClient
                    .getSubscription(dto.getSubscriptionId().toString());
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException(
                    "Subscription service is unavailable"
            );
        }

        if (!"ACTIVE".equals(sub.getStatus())) {
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

            if ("succeeded".equals(status)) {

                String metric = "api_calls";
                long quantity = 1L;

                BigDecimal unitPrice =
                        invoice.getAmount().divide(BigDecimal.valueOf(quantity));

                usageChargeService.createUsageCharge(
                        invoice.getInvoiceId(),
                        metric,
                        quantity,
                        unitPrice
                );
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

    public List<Subscription.SubscriptionResponse> getActiveSubscriptions(UUID userId) {

        try {
            Subscription.GetUserActiveSubscriptionsResponse response =
                    subscriptionGrpcClient
                            .getUserActiveSubscriptions(userId.toString());

            return response.getSubscriptionsList();

        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException(
                    "Subscription service is unavailable"
            );
        }
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

    @Transactional
    public void handleSubscriptionUpdate(SubscriptionUpdated event) {

        UUID subscriptionId =
                UUID.fromString(event.getSubscriptionId());

        SubscriptionStatus status = event.getStatus();

        switch (status) {

            case ACTIVE -> invoiceRepository
                    .findBySubscriptionId(subscriptionId)
                    .ifPresent(invoice -> {
                        if (InvoiceStatus.DRAFT.name()
                                .equals(invoice.getStatus())) {
                            invoice.setStatus(InvoiceStatus.ISSUED.name());
                        }
                    });

            case CANCELED, EXPIRED -> invoiceRepository
                    .findBySubscriptionId(subscriptionId)
                    .ifPresent(invoice -> {
                        invoice.setStatus(InvoiceStatus.FAILED.name());
                        invoiceRepository.save(invoice);
                    });

            case TRIALING -> {
            }
        }
    }
}
