package com.project.billing.adapter.out.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduler so the transactional-outbox relay runs.
 *
 * <p>{@link com.project.billing.adapter.out.messaging.BillingEventRelay} is a scheduler-driven
 * bean: nothing injects it, and its {@code @Scheduled relay()} is invoked on a fixed delay by the
 * scheduler this config turns on. It is a {@code @Component} (so it is component-scanned and receives
 * its own dependencies) and is active in every profile except {@code test}. This config exists on its
 * own — rather than piggy-backing on the Kafka producer config — so the relay's activation is
 * discoverable next to the relay itself.
 */
@Configuration
@EnableScheduling
public class EventRelayConfig {
}
