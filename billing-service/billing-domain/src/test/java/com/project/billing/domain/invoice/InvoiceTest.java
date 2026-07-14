package com.project.billing.domain.invoice;

import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceTest {

    @Test
    void issueInitialCreatesIssuedInvoiceWithDefaults() {
        Invoice invoice = Invoice.issueInitial(
                SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID()));

        assertEquals(InvoiceStatus.ISSUED, invoice.status());
        assertEquals(0, invoice.amount().amount().compareTo(new java.math.BigDecimal("29.99")));
        assertEquals("USD", invoice.amount().currency());
        assertTrue(invoice.isNew());
        assertTrue(invoice.dueAt().isAfter(invoice.issuedAt()));
    }

    @Test
    void activateOnlyPromotesDraft() {
        Invoice draft = Invoice.create(
                SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID()),
                com.project.billing.domain.shared.Money.of("5", "USD"),
                InvoiceStatus.DRAFT, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusDays(1));

        draft.activate();
        assertEquals(InvoiceStatus.ISSUED, draft.status());

        draft.activate();
        assertEquals(InvoiceStatus.ISSUED, draft.status());
    }

    @Test
    void markFailedSetsFailedStatus() {
        Invoice invoice = Invoice.issueInitial(
                SubscriptionId.of(UUID.randomUUID()), CustomerId.of(UUID.randomUUID()));
        invoice.markFailed();
        assertEquals(InvoiceStatus.FAILED, invoice.status());
    }
}
