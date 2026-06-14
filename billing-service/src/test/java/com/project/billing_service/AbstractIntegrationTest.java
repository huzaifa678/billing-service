package com.project.billing_service;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for Spring Boot integration tests.
 *
 * <p>Owns the shared infrastructure — Kafka, PostgreSQL and Redis Testcontainers
 * wired in via {@code @ServiceConnection} — plus the common test profile and
 * database auto-configuration. Concrete tests extend this class and declare only
 * the beans and {@code @SpringBootTest} variant they need, instead of repeating
 * the container setup in every test (the enterprise base-class test pattern).
 */
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    @ServiceConnection
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("test")
                    .withUsername("test")
                    .withPassword("test");

    @ServiceConnection
    @Container
    static final GenericContainer REDIS =
            new GenericContainer(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
}
