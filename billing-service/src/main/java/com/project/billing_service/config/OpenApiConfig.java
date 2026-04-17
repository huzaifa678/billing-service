package com.project.billing_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Billing Service API")
                        .description("REST API for Billing service in the SAAS platform")
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local server")
                ));
    }
}
