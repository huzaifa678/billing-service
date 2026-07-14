package com.project.billing.application.invoice.service;

import com.project.billing.application.invoice.port.in.CreateInitialInvoiceCommand;
import com.project.billing.application.invoice.port.in.CreateInitialInvoiceUseCase;
import com.project.billing.application.invoice.port.in.CreateInvoiceCommand;
import com.project.billing.application.invoice.port.in.CreateInvoiceUseCase;
import com.project.billing.application.invoice.port.in.HandleSubscriptionUpdateUseCase;
import com.project.billing.application.invoice.port.in.PayInvoiceCommand;
import com.project.billing.application.invoice.port.in.PayInvoiceUseCase;
import com.project.billing.application.invoice.port.in.SubscriptionUpdateCommand;
import com.project.billing.application.invoice.port.out.InvoiceRepositoryPort;
import com.project.billing.application.invoice.port.out.PaymentPort;
import com.project.billing.application.invoice.port.out.PaymentResult;
import com.project.billing.application.invoice.port.out.RateLimiterPort;
import com.project.billing.application.invoice.port.out.SubscriptionGatewayPort;
import com.project.billing.application.usage.port.in.RecordUsageChargeCommand;
import com.project.billing.application.usage.port.in.RecordUsageChargeUseCase;
import com.project.billing.domain.exception.InvalidSubscriptionStateException;
import com.project.billing.domain.exception.InvoiceNotFoundException;
import com.project.billing.domain.exception.PaymentFailedException;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import com.project.billing.domain.subscription.SubscriptionSnapshot;
import com.project.billing.domain.usage.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command side for invoices. Orchestrates the aggregate lifecycle (create, pay,
 * initial issue, subscription-driven activation/failure) and delegates payment,
 * subscription lookups, rate limiting and usage recording to outbound ports.
 * Replaces the write responsibilities of the former {@code BillingService} and
 * {@code SubscriptionUpdateHandler}.
 */
@Service
@RequiredArgsConstructor
public class InvoiceCommandService implements
        CreateInvoiceUseCase,
        PayInvoiceUseCase,
        CreateInitialInvoiceUseCase,
        HandleSubscriptionUpdateUseCase {

    private static final Metric USAGE_METRIC_API_CALLS = Metric.of("api_calls");
    private static final long USAGE_QUANTITY = 1L;

    private final InvoiceRepositoryPort invoiceRepository;
    private final PaymentPort paymentPort;
    private final SubscriptionGatewayPort subscriptionGateway;
    private final RateLimiterPort rateLimiter;
    private final RecordUsageChargeUseCase recordUsageChargeUseCase;

    @Override
    public Invoice create(CreateInvoiceCommand command) {
        SubscriptionSnapshot subscription =
                subscriptionGateway.getSubscription(SubscriptionId.of(command.subscriptionId()));

        if (!subscription.isActive()) {
            throw new InvalidSubscriptionStateException(
                    "Cannot create invoice for inactive subscription"
            );
        }

        Invoice invoice = Invoice.create(
                SubscriptionId.of(command.subscriptionId()),
                CustomerId.of(command.customerId()),
                Money.of(command.amount(), command.currency()),
                parseStatus(command.status()),
                command.issuedAt(),
                command.dueAt()
        );

        return invoiceRepository.save(invoice);
    }

    @Override
    public String pay(PayInvoiceCommand command) {
        InvoiceId invoiceId = InvoiceId.of(command.invoiceId());

        rateLimiter.checkPayInvoice(invoiceId);

        Invoice invoice = loadInvoice(invoiceId);

        try {
            PaymentResult result = paymentPort.pay(invoice, command.paymentMethodId());

            if (result.isSuccessful()) {
                invoice.markPaid();
                invoiceRepository.save(invoice);
                recordUsageCharge(invoice);
            }

            return result.status();

        } catch (RuntimeException e) {
            invoice.markFailed();
            invoiceRepository.save(invoice);

            throw new PaymentFailedException(
                    "Payment failed for invoice " + invoiceId + e
            );
        }
    }

    @Override
    @Transactional
    public void createInitial(CreateInitialInvoiceCommand command) {
        SubscriptionId subscriptionId = SubscriptionId.of(command.subscriptionId());

        invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(existing -> {
                    throw new InvalidSubscriptionStateException(
                            "Invoice already exists for subscription " + subscriptionId
                    );
                });

        Invoice invoice = Invoice.issueInitial(
                subscriptionId,
                CustomerId.of(command.customerId())
        );

        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void handle(SubscriptionUpdateCommand command) {
        SubscriptionId subscriptionId = SubscriptionId.of(command.subscriptionId());

        switch (command.status()) {
            case ACTIVE -> activateInvoice(subscriptionId);
            case CANCELED, EXPIRED -> failInvoice(subscriptionId);
            case TRIALING, PAST_DUE -> {
                // no invoice change for these transitions
            }
        }
    }

    private void activateInvoice(SubscriptionId subscriptionId) {
        invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(invoice -> {
                    invoice.activate();
                    invoiceRepository.save(invoice);
                });
    }

    private void failInvoice(SubscriptionId subscriptionId) {
        invoiceRepository.findBySubscriptionId(subscriptionId)
                .ifPresent(invoice -> {
                    invoice.markFailed();
                    invoiceRepository.save(invoice);
                });
    }

    private void recordUsageCharge(Invoice invoice) {
        Money unitPrice = invoice.amount().divide(USAGE_QUANTITY);

        recordUsageChargeUseCase.record(new RecordUsageChargeCommand(
                invoice.id(),
                USAGE_METRIC_API_CALLS,
                USAGE_QUANTITY,
                unitPrice
        ));
    }

    private Invoice loadInvoice(InvoiceId invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(
                        "Invoice not found with id: " + invoiceId
                ));
    }

    private InvoiceStatus parseStatus(String status) {
        return status == null ? InvoiceStatus.DRAFT : InvoiceStatus.valueOf(status);
    }
}
