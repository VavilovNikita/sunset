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
 * <p>Flyway alone CANNOT bootstrap this schema on a database that was never touched by Prisma:
 * migration {@code V1} is an intentionally empty placeholder (the original tables were created by
 * Prisma, years before this app owned the schema - see {@code V1}'s own description), and relied
 * on {@code baseline-on-migrate} adopting Prisma's already-existing schema. {@code
 * V1_1__prisma_baseline_schema.sql} now recreates that Prisma-era schema for a genuinely empty
 * database instead (see its own comment), so this file no longer strictly needs a pre-Prisma
 * schema to adopt - but it's kept anyway as a plain optimization: test-db-baseline.sql (a {@code
 * pg_dump --schema-only} of a fully-migrated database, plus just the data rows of {@code
 * flyway_schema_history} - no business data) is loaded into the container first, so Flyway sees a
 * schema already at the version captured below and only needs to apply whatever migrations come
 * after that, instead of re-running the entire history through Testcontainers on every test JVM
 * boot. This file does NOT need regenerating after every new migration - Flyway applies anything
 * newer than the captured version on top of the baseline normally; only refresh it (see the
 * regeneration steps in this fix's own history for the exact procedure - a scratch Postgres
 * container, boot the app so Flyway applies every migration, then {@code pg_dump}) if the already-
 * applied migrations themselves ever change (they shouldn't) or to fold newer migrations in for
 * tidiness. Currently captured at V90 (V1 through V90, 91 migrations including {@code V1_1}).
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
            .withInitScript("test-db-baseline.sql")
            // Postgres's default max_connections (100) is shared across every distinct Spring
            // context this suite creates - each unique combination of @DynamicPropertySource
            // values/bean overrides across ~60+ @SpringBootTest classes gets its own cached
            // ApplicationContext, and therefore its own HikariCP pool, against this one container.
            // The suite runs close enough to that ceiling that adding even one more genuinely
            // distinct full-context test class can tip an unrelated, alphabetically-later class
            // over into "FATAL: sorry, too many clients already" - reproduced deterministically,
            // not flaky, since Surefire's run order is stable. Raised well above what this suite
            // could plausibly need, rather than tuned to the current count, so this doesn't need
            // revisiting every time a future test adds one more context.
            .withCommand("postgres", "-c", "max_connections=300");

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
