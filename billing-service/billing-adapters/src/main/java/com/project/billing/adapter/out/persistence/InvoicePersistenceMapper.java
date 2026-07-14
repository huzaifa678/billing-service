package com.project.billing.adapter.out.persistence;

import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import org.springframework.stereotype.Component;

/** Maps between the {@link Invoice} aggregate and its JPA persistence model. */
@Component
public class InvoicePersistenceMapper {

    public InvoiceJpaEntity toJpa(Invoice invoice) {
        return new InvoiceJpaEntity(
                invoice.id().value(),
                invoice.subscriptionId().value(),
                invoice.customerId().value(),
                invoice.amount().amount(),
                invoice.amount().currency(),
                invoice.status().name(),
                invoice.issuedAt(),
                invoice.dueAt(),
                invoice.isNew()
        );
    }

    public Invoice toDomain(InvoiceJpaEntity entity) {
        return Invoice.reconstitute(
                InvoiceId.of(entity.getInvoiceId()),
                SubscriptionId.of(entity.getSubscriptionId()),
                CustomerId.of(entity.getCustomerId()),
                Money.of(entity.getAmount(), entity.getCurrency()),
                InvoiceStatus.valueOf(entity.getStatus()),
                entity.getIssuedAt(),
                entity.getDueAt()
        );
    }
}
