package com.project.billing.application.invoice.port.in;

import com.project.billing.domain.invoice.Invoice;

import java.util.List;
import java.util.UUID;

/** Inbound query port (read side) for invoices. */
public interface InvoiceQueries {

    Invoice getById(UUID invoiceId);

    List<Invoice> byCustomer(UUID customerId);

    List<Invoice> byStatus(String status);
}
