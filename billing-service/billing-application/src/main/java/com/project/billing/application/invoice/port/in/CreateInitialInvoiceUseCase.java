package com.project.billing.application.invoice.port.in;

/** Inbound port: create the default initial invoice for a new subscription. */
public interface CreateInitialInvoiceUseCase {

    void createInitial(CreateInitialInvoiceCommand command);
}
