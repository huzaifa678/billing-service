package com.project.billing_service.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppenderConfig {

    @Bean
    public boolean otelAppenderInstaller(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
        return true;
    }
}
