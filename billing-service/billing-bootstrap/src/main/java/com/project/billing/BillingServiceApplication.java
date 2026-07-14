package com.project.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point. The base package {@code com.project.billing} covers the
 * domain, application and adapter modules, so component scanning, entity scanning
 * and Spring Data repository discovery all resolve without extra configuration.
 */
@SpringBootApplication
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
