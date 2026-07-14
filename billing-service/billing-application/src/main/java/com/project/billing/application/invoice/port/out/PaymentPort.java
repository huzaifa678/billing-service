package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.Invoice;

/**
 * Outbound port for charging an invoice through a payment provider.
 * Implementations translate provider-specific failures into runtime exceptions;
 * the pay-invoice use case treats any failure as a payment failure.
 */
public interface PaymentPort {

    PaymentResult pay(Invoice invoice, String paymentMethodId);
}
