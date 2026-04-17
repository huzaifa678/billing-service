package com.project.billing_service.service;

import com.project.billing_service.model.entities.InvoiceEntity;

public interface BillingInterface {
    String payInvoice(InvoiceEntity invoice, String paymentMethodId) throws Exception;
}
