package com.project.billing.adapter.out.subscription;

import com.google.protobuf.Timestamp;
import com.project.billing.application.exception.SubscriptionServiceUnavailableException;
import com.project.billing.application.invoice.port.out.SubscriptionGatewayPort;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.SubscriptionId;
import com.project.billing.domain.subscription.SubscriptionSnapshot;
import com.project.subscription.v1.GetSubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Adapter fulfilling {@link SubscriptionGatewayPort} via the gRPC client. Owns the
 * translation of the transport model ({@link GetSubscriptionResponse}) into the
 * domain {@link SubscriptionSnapshot} and of transport failures into the
 * application-level {@link SubscriptionServiceUnavailableException}.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionGrpcAdapter implements SubscriptionGatewayPort {

    private final SubscriptionGrpcClient client;

    @Override
    public SubscriptionSnapshot getSubscription(SubscriptionId subscriptionId) {
        try {
            return toSnapshot(client.getSubscription(subscriptionId.value().toString()));
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException(
                    "Subscription service is unavailable"
            );
        }
    }

    @Override
    public List<SubscriptionSnapshot> getActiveSubscriptions(CustomerId customerId) {
        try {
            return client.getUserActiveSubscriptions(customerId.value().toString())
                    .getSubscriptionsList()
                    .stream()
                    .map(this::toSnapshot)
                    .toList();
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException(
                    "Subscription service is unavailable"
            );
        }
    }

    private SubscriptionSnapshot toSnapshot(GetSubscriptionResponse response) {
        return new SubscriptionSnapshot(
                response.getId(),
                response.getUserId(),
                response.getPlanId(),
                response.getStatus(),
                toInstant(response.getCurrentPeriodStart()),
                toInstant(response.getCurrentPeriodEnd()),
                response.getCancelAtPeriodEnd(),
                toInstant(response.getCreatedAt()),
                toInstant(response.getUpdatedAt())
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
