package com.project.billing.application.invoice.port.out;

import com.project.billing.domain.invoice.InvoiceId;

/** Outbound port for rate limiting sensitive operations. */
public interface RateLimiterPort {

    void checkPayInvoice(InvoiceId invoiceId);

    void checkSubscriptionEvent();
}
