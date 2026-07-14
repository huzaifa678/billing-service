package com.project.billing.adapter.out.subscription;

import com.project.subscription.v1.GetSubscriptionRequest;
import com.project.subscription.v1.GetSubscriptionResponse;
import com.project.subscription.v1.GetUserActiveSubscriptionsRequest;
import com.project.subscription.v1.GetUserActiveSubscriptionsResponse;
import com.project.subscription.v1.SubscriptionServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    public GetSubscriptionResponse getSubscription(String subscriptionId) {
        GetSubscriptionRequest request =
                GetSubscriptionRequest.newBuilder()
                        .setSubscriptionId(subscriptionId)
                        .build();

        return stub.getSubscription(request);
    }

    @Retry(name = "subscriptionGrpc", fallbackMethod = "getUserActiveSubscriptionsFallback")
    @CircuitBreaker(name = "subscriptionGrpc", fallbackMethod = "getUserActiveSubscriptionsFallback")
    public GetUserActiveSubscriptionsResponse getUserActiveSubscriptions(String userId) {
        GetUserActiveSubscriptionsRequest request =
                GetUserActiveSubscriptionsRequest.newBuilder()
                        .setUserId(userId)
                        .build();

        return stub.getUserActiveSubscriptions(request);
    }

    private GetSubscriptionResponse subscriptionFallback(String id, Throwable ex) throws ServiceUnavailableException {
        log.error("Subscription service call failed for id: {}", id, ex);
        throw new ServiceUnavailableException(
                "Subscription service unavailable for " + id
        );
    }

    private GetUserActiveSubscriptionsResponse getUserActiveSubscriptionsFallback(String userId, Throwable ex) throws ServiceUnavailableException {
        throw new ServiceUnavailableException(
                "Subscription service unavailable while fetching active subscriptions for user " + userId
        );
    }
}
