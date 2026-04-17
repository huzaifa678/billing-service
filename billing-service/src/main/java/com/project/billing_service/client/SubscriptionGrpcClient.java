package com.project.billing_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import subscription.Subscription;
import subscription.SubscriptionServiceGrpc;

import javax.naming.ServiceUnavailableException;

@Slf4j
@Component
public class SubscriptionGrpcClient {

    private final SubscriptionServiceGrpc.SubscriptionServiceBlockingStub stub;

    public SubscriptionGrpcClient(ManagedChannel subscriptionChannel) {
        this.stub = SubscriptionServiceGrpc.newBlockingStub(subscriptionChannel);
    }

    @Retry(name = "subscriptionGrpc", fallbackMethod = "subscriptionFallback")
    @CircuitBreaker(name = "subscriptionGrpc", fallbackMethod = "subscriptionFallback")
    public Subscription.SubscriptionResponse getSubscription(String subscriptionId) {
        Subscription.GetSubscriptionRequest request =
                Subscription.GetSubscriptionRequest.newBuilder()
                        .setSubscriptionId(subscriptionId)
                        .build();

        return stub.getSubscription(request);
    }

    @Retry(name = "subscriptionGrpc", fallbackMethod = "getUserActiveSubscriptionsFallback")
    @CircuitBreaker(name = "subscriptionGrpc", fallbackMethod = "getUserActiveSubscriptionsFallback")
    public Subscription.GetUserActiveSubscriptionsResponse getUserActiveSubscriptions(String userId) {
        Subscription.GetUserActiveSubscriptionsRequest request =
                Subscription.GetUserActiveSubscriptionsRequest.newBuilder()
                        .setUserId(userId)
                        .build();

        return stub.getUserActiveSubscriptions(request);
    }

    private Subscription.SubscriptionResponse subscriptionFallback(String id, Throwable ex) throws ServiceUnavailableException {
        log.error("Subscription service call failed for id: {}", id, ex);
        throw new ServiceUnavailableException(
                "Subscription service unavailable for " + id
        );
    }

    private Subscription.GetUserActiveSubscriptionsResponse getUserActiveSubscriptionsFallback(String userId, Throwable ex) throws ServiceUnavailableException {
        throw new ServiceUnavailableException(
                "Subscription service unavailable while fetching active subscriptions for user " + userId
        );
    }
}

