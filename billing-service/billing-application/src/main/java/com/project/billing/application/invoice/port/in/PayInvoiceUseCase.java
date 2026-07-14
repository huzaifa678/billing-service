package com.project.billing.application.invoice.port.in;

/** Inbound port: pay an invoice and return the payment provider status. */
public interface PayInvoiceUseCase {

    String pay(PayInvoiceCommand command);
}
