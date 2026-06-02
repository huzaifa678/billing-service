package com.project.billing_service.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.project.subscription.v1.GetSubscriptionRequest;
import com.project.subscription.v1.GetSubscriptionResponse;
import com.project.subscription.v1.GetUserActiveSubscriptionsRequest;
import com.project.subscription.v1.GetUserActiveSubscriptionsResponse;
import com.project.subscription.v1.SubscriptionServiceGrpc;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "subscription-service", pactVersion = PactSpecVersion.V4)
@MockServerConfig(
        implementation = MockServerImplementation.Plugin,
        registryEntry = "protobuf/transport/grpc"
)
class SubscriptionGrpcClientPactTest {

    private static final String PROTO_PATH =
            Paths.get("src/test/resources/proto/subscription/v1/subscription.proto")
                    .toAbsolutePath().toString();

    private static final String PROTO_INCLUDE_DIR =
            Paths.get("src/test/resources/proto").toAbsolutePath().toString();

    @Pact(consumer = "billing-service")
    V4Pact getSubscriptionPact(PactBuilder builder) {
        return builder
                .usingPlugin("protobuf")
                .given("subscription 550e8400-e29b-41d4-a716-446655440000 exists and is ACTIVE")
                .expectsToReceive("a GetSubscription request for an ACTIVE subscription",
                        "core/interaction/synchronous-message")
                .with(Map.of(
                        "pact:proto", PROTO_PATH,
                        "pact:proto-service", "SubscriptionService/GetSubscription",
                        "pact:content-type", "application/grpc",
                        "request", Map.of(
                                "subscription_id", "matching(regex, '^[0-9a-fA-F-]{36}$', '550e8400-e29b-41d4-a716-446655440000')"
                        ),
                        "response", List.of(Map.of(
                                "id", "matching(regex, '^[0-9a-fA-F-]{36}$', '550e8400-e29b-41d4-a716-446655440000')",
                                "user_id", "matching(regex, '^[0-9a-fA-F-]{36}$', '9f1c2d3e-7a8b-4c5d-9e0f-123456789abc')",
                                "plan_id", "matching(type, 'plan-basic')",
                                "status", "matching(regex, '^(ACTIVE|INACTIVE|CANCELLED|PAUSED)$', 'ACTIVE')",
                                "cancel_at_period_end", "matching(boolean, false)"
                        ))
                ))
                .toPact();
    }

    @Pact(consumer = "billing-service")
    V4Pact getUserActiveSubscriptionsPact(PactBuilder builder) {
        return builder
                .usingPlugin("protobuf")
                .given("user 9f1c2d3e-7a8b-4c5d-9e0f-123456789abc has one ACTIVE subscription")
                .expectsToReceive("a GetUserActiveSubscriptions request for a user with one active subscription",
                        "core/interaction/synchronous-message")
                .with(Map.of(
                        "pact:proto", PROTO_PATH,
                        "pact:proto-service", "SubscriptionService/GetUserActiveSubscriptions",
                        "pact:content-type", "application/grpc",
                        "request", Map.of(
                                "user_id", "matching(regex, '^[0-9a-fA-F-]{36}$', '9f1c2d3e-7a8b-4c5d-9e0f-123456789abc')"
                        ),
                        "response", List.of(Map.of(
                                "subscriptions", Map.of(
                                        "pact:match", "eachValue(matching($'subscription'))",
                                        "pact:min", 1,
                                        "subscription", Map.of(
                                                "id", "matching(regex, '^[0-9a-fA-F-]{36}$', '550e8400-e29b-41d4-a716-446655440000')",
                                                "user_id", "matching(regex, '^[0-9a-fA-F-]{36}$', '9f1c2d3e-7a8b-4c5d-9e0f-123456789abc')",
                                                "plan_id", "matching(type, 'plan-basic')",
                                                "status", "matching(regex, '^(ACTIVE|INACTIVE|CANCELLED|PAUSED)$', 'ACTIVE')",
                                                "cancel_at_period_end", "matching(boolean, false)"
                                        )
                                )
                        ))
                ))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getSubscriptionPact")
    void getSubscription_returnsActiveSubscription(MockServer mockServer) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", mockServer.getPort())
                .usePlaintext()
                .build();
        try {
            var stub = SubscriptionServiceGrpc.newBlockingStub(channel);

            var response = stub.getSubscription(
                    GetSubscriptionRequest.newBuilder()
                            .setSubscriptionId("550e8400-e29b-41d4-a716-446655440000")
                            .build()
            );

            assertNotNull(response);
            assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getId());
            assertEquals("9f1c2d3e-7a8b-4c5d-9e0f-123456789abc", response.getUserId());
            assertEquals("plan-basic", response.getPlanId());
            assertEquals("ACTIVE", response.getStatus());
            assertFalse(response.getCancelAtPeriodEnd());
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    @PactTestFor(pactMethod = "getUserActiveSubscriptionsPact")
    void getUserActiveSubscriptions_returnsSubscriptionList(MockServer mockServer) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", mockServer.getPort())
                .usePlaintext()
                .build();
        try {
            var stub = SubscriptionServiceGrpc.newBlockingStub(channel);

            var response = stub.getUserActiveSubscriptions(
                    GetUserActiveSubscriptionsRequest.newBuilder()
                            .setUserId("9f1c2d3e-7a8b-4c5d-9e0f-123456789abc")
                            .build()
            );

            assertNotNull(response);
            assertFalse(response.getSubscriptionsList().isEmpty());
            assertEquals("ACTIVE", response.getSubscriptions(0).getStatus());
            assertEquals("9f1c2d3e-7a8b-4c5d-9e0f-123456789abc", response.getSubscriptions(0).getUserId());
        } finally {
            channel.shutdownNow();
        }
    }

}
