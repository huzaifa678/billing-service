package com.project.billing_service.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(
        name = "spring.grpc.client.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ChannelConfig {
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
            name = "spring.grpc.client.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ManagedChannel subscriptionChannel(
            @Value("${grpc.subscription.host}") String host,
            @Value("${grpc.subscription.port}") int port
    ) {
        return ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
    }
}
