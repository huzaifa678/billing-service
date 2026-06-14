package com.project.billing_service.service;

import com.project.subscription.v1.GetSubscriptionResponse;

import java.util.List;


public interface SubscriptionGateway {

    GetSubscriptionResponse getSubscription(String subscriptionId);

    List<GetSubscriptionResponse> getActiveSubscriptions(String userId);
}
