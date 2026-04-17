package com.project.billing_service;

import io.grpc.stub.StreamObserver;
import subscription.Subscription;
import subscription.SubscriptionServiceGrpc;

public class FakeSubscriptionService extends SubscriptionServiceGrpc.SubscriptionServiceImplBase {

    @Override
    public void getSubscription(
            Subscription.GetSubscriptionRequest request,
            StreamObserver<Subscription.SubscriptionResponse> responseObserver
    ) {

        String status = "ACTIVE";

        if (request.getSubscriptionId().contains("inactive")) {
            status = "CANCELED";
        }

        responseObserver.onNext(
                Subscription.SubscriptionResponse.newBuilder()
                        .setStatus(status)
                        .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void getUserActiveSubscriptions(
            Subscription.GetUserActiveSubscriptionsRequest request,
            StreamObserver<Subscription.GetUserActiveSubscriptionsResponse> responseObserver
    ) {

        Subscription.SubscriptionResponse sub =
                Subscription.SubscriptionResponse.newBuilder()
                        .setStatus("ACTIVE")
                        .build();

        responseObserver.onNext(
                Subscription.GetUserActiveSubscriptionsResponse.newBuilder()
                        .addSubscriptions(sub)
                        .build()
        );

        responseObserver.onCompleted();
    }
}
