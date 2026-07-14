package com.project.billing.application.invoice.port.in;

import com.project.billing.domain.subscription.SubscriptionSnapshot;

import java.util.List;
import java.util.UUID;

/** Inbound query port (read side) for subscription data used by billing. */
public interface SubscriptionQueries {

    List<SubscriptionSnapshot> activeByUser(UUID userId);
}
