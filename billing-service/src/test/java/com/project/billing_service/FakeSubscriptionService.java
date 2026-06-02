package com.project.billing_service;

import io.grpc.stub.StreamObserver;
import com.project.subscription.v1.GetSubscriptionRequest;
import com.project.subscription.v1.GetSubscriptionResponse;
import com.project.subscription.v1.GetUserActiveSubscriptionsRequest;
import com.project.subscription.v1.GetUserActiveSubscriptionsResponse;
import com.project.subscription.v1.SubscriptionServiceGrpc;

public class FakeSubscriptionService extends SubscriptionServiceGrpc.SubscriptionServiceImplBase {

    @Override
    public void getSubscription(
            GetSubscriptionRequest request,
            StreamObserver<GetSubscriptionResponse> responseObserver
    ) {

        String status = "ACTIVE";

        if (request.getSubscriptionId().contains("inactive")) {
            status = "CANCELED";
        }

        responseObserver.onNext(
                GetSubscriptionResponse.newBuilder()
                        .setStatus(status)
                        .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void getUserActiveSubscriptions(
            GetUserActiveSubscriptionsRequest request,
            StreamObserver<GetUserActiveSubscriptionsResponse> responseObserver
    ) {

        GetSubscriptionResponse sub =
                GetSubscriptionResponse.newBuilder()
                        .setStatus("ACTIVE")
                        .build();

        responseObserver.onNext(
                GetUserActiveSubscriptionsResponse.newBuilder()
                        .addSubscriptions(sub)
                        .build()
        );

        responseObserver.onCompleted();
    }
}