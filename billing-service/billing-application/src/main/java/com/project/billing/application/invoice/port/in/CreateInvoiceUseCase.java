package com.project.billing.application.invoice.port.in;

import com.project.billing.domain.invoice.Invoice;

/** Inbound port: create an invoice for an active subscription. */
public interface CreateInvoiceUseCase {

    Invoice create(CreateInvoiceCommand command);
}
