package com.ziyara.backend;

import com.ziyara.core.TestcontainersConfiguration;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for full-stack integration tests.
 * Provides a Testcontainers-backed PostgreSQL database via {@link TestcontainersConfiguration}.
 * Subclasses inherit the {@code @Tag("docker")} marker so they are excluded from the
 * default {@code ./gradlew test} run and only execute with {@code -PrunDockerTests}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("docker")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate rest;
}
