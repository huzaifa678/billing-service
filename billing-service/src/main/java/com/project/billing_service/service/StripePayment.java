package com.project.billing_service.service;

import com.project.billing_service.model.entities.InvoiceEntity;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class StripePayment implements BillingInterface{

    private final RateLimiterService rateLimiterService;

    @Override
    @Retry(name = "stripePayment")
    public String payInvoice(InvoiceEntity invoice, String paymentMethodId) throws StripeException {
        rateLimiterService.checkPayInvoice(invoice.getInvoiceId());
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(invoice.getAmount().multiply(new BigDecimal("100")).longValue()) // Stripe in cents
                .setCurrency(invoice.getCurrency().toLowerCase())
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                )
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        return intent.getStatus();
    }
}
