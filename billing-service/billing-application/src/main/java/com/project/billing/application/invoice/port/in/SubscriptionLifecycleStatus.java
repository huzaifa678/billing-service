package com.project.billing.application.invoice.port.in;

/**
 * Subscription lifecycle status as understood by the billing context.
 * Messaging adapters map their transport enum (Avro {@code SubscriptionStatus})
 * onto this so application logic stays free of Avro types.
 */
public enum SubscriptionLifecycleStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    EXPIRED
}
