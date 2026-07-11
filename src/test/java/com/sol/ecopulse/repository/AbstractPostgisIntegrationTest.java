package com.sol.ecopulse.repository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for repository integration tests that exercise the PostGIS
 * native queries against a real PostgreSQL/PostGIS instance.
 *
 * <p>The container is started once per JVM (singleton pattern) and shared across
 * all subclasses; Testcontainers' Ryuk reaper stops it when the JVM exits. Each
 * test runs inside a transaction that rolls back, so tests stay isolated.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class AbstractPostgisIntegrationTest {

    // PostGIS image (declared linux/amd64 in docker-compose). asCompatibleSubstituteFor lets
    // Testcontainers treat it as a drop-in "postgres" container.
    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:15-3.3").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
