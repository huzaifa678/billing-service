package com.project.billing_service.client;

import com.project.billing_service.exceptions.SubscriptionServiceUnavailableException;
import com.project.billing_service.service.SubscriptionGateway;
import com.project.subscription.v1.GetSubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that fulfils the {@link SubscriptionGateway} port using the gRPC
 * client. It owns the translation of transport-level failures into the domain
 * {@link SubscriptionServiceUnavailableException}, removing the duplicated
 * try/catch that previously lived in the billing service.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionGrpcGateway implements SubscriptionGateway {

    private final SubscriptionGrpcClient client;

    @Override
    public GetSubscriptionResponse getSubscription(String subscriptionId) {
        try {
            return client.getSubscription(subscriptionId);
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException(
                    "Subscription service is unavailable"
            );
        }
    }

    @Override
    public List<GetSubscriptionResponse> getActiveSubscriptions(String userId) {
        try {
            return client.getUserActiveSubscriptions(userId)
                    .getSubscriptionsList();
        } catch (Exception ex) {
            throw new SubscriptionServiceUnavailableException(
                    "Subscription service is unavailable"
            );
        }
    }
}
