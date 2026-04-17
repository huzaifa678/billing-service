package com.project.billing_service.config;

import com.project.billing_service.FakeSubscriptionService;
import io.grpc.*;
import io.grpc.inprocess.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class GrpcTestConfig {

    private static final String SERVER_NAME = "test-grpc";

    @Bean
    public ManagedChannel managedChannel() throws Exception {

        Server server = InProcessServerBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .addService(new FakeSubscriptionService())
                .build().start();

        return InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();
    }
}