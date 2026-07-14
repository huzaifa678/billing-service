package com.project.billing.adapter.out.payment;

import com.project.billing.application.invoice.port.out.PaymentPort;
import com.project.billing.application.invoice.port.out.PaymentResult;
import com.project.billing.domain.invoice.Invoice;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe-backed implementation of {@link PaymentPort}. Rate limiting was removed
 * from here — it is now enforced once in the pay-invoice use case. On a Stripe
 * failure a {@link PaymentGatewayException} is thrown so the {@code @Retry} proxy
 * can retry and, if still failing, the use case can mark the invoice failed.
 */
@Component
public class StripePaymentAdapter implements PaymentPort {

    private static final BigDecimal CENTS_PER_UNIT = new BigDecimal("100");

    @Override
    @Retry(name = "stripePayment")
    public PaymentResult pay(Invoice invoice, String paymentMethodId) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(invoice.amount().amount().multiply(CENTS_PER_UNIT).longValue())
                    .setCurrency(invoice.amount().currency().toLowerCase())
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

            return new PaymentResult(intent.getStatus());

        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe payment failed", e);
        }
    }
}
