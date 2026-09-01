package com.sunsetbeach;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Every {@code @SpringBootTest} class extends this instead of connecting to whatever Postgres
 * happens to be running on localhost:5434 - the same instance a developer clicks around in by
 * hand during manual testing. That sharing is what caused real failures from leftover manual
 * fixture data and forced hand-cleanup after test runs (see the migration this class enabled).
 *
 * One container, started once (the static initializer runs the first time any subclass is
 * loaded), for the entire test JVM - not one container per test class. Every subclass registers
 * the exact same dynamic datasource properties, so Spring's test-context cache recognizes them
 * as the same configuration and reuses one Spring context too, rather than restarting it 25
 * times.
 *
 * <p>Flyway alone CANNOT bootstrap this schema on an empty database: migration {@code V1} is an
 * intentionally empty placeholder (the original tables were created by Prisma, years before this
 * app owned the schema - see {@code V1}'s own description). That's harmless when restoring a real
 * backup (the dump carries the physical schema regardless of what created it - see
 * backup/README.md), but a genuinely empty Testcontainers database has nothing for {@code V1} to
 * be a no-op *of*. test-db-baseline.sql (a {@code pg_dump --schema-only} of the dev database, plus
 * just the data rows of {@code flyway_schema_history} - no business data) is loaded into the
 * container first, so Flyway sees a schema already at version 20 and only needs to apply whatever
 * migrations come after that, exactly like it would against any real environment. This file does
 * NOT need regenerating after every new migration - Flyway applies anything newer than V20 on top
 * of the baseline normally; only refresh it if V1-V20 themselves ever change (they shouldn't) or
 * to fold newer migrations in for tidiness.
 *
 * <p>No explicit stop() call: Testcontainers' own Ryuk reaper container removes this one when the
 * JVM exits (test run end, or a killed/crashed run), so there's nothing to leak even if a run is
 * interrupted - the opposite of the shared dev database, which stays however a crashed run left
 * it.
 */
@Tag("integration")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sunsetbeach")
            .withUsername("sunsetbeach")
            .withPassword("sunsetbeach")
            .withInitScript("test-db-baseline.sql");

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
