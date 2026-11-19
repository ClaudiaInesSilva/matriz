package com.matriz.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class LocalDatabaseUrlGuardTest {
    private static final String COMPOSE_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5433/matriz_compose";
    private static final String COMPOSE_DATABASE_USERNAME = "matriz_compose_app";

    @Test
    void permitsSupportedLoopbackPostgresUrlsAndPinsTheValidatedUrl() {
        for (String jdbcUrl : new String[] {
            "jdbc:postgresql://localhost:5432/matriz_db",
            "jdbc:postgresql://localhost",
            "jdbc:postgresql://localhost/matriz_db",
            "jdbc:postgresql://127.0.0.1:5432/matriz_db",
            "jdbc:postgresql://[::1]:5432/matriz_db"
        }) {
            MockEnvironment environment = environmentWithUrl(jdbcUrl);

            assertEquals(jdbcUrl, LocalDatabaseUrlGuard.validateAndPin(environment));
            assertEquals(jdbcUrl, environment.getProperty("spring.datasource.url"));
            assertFalse(environment.getProperty("spring.flyway.enabled", Boolean.class, true));
        }
    }

    @Test
    void rejectsRemoteUrlFromCommandLineEnvironmentSystemOrExternalConfiguration() {
        for (String sourceName : new String[] {
            "commandLineArgs",
            "systemEnvironment",
            "systemProperties",
            "externalConfig"
        }) {
            MockEnvironment environment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
            environment.getPropertySources().addFirst(new MapPropertySource(
                    sourceName, Map.of("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/matriz_db")));

            assertThrows(IllegalArgumentException.class,
                    () -> LocalDatabaseUrlGuard.validateAndPin(environment), sourceName);
        }
    }

    @Test
    void rejectsRemoteUrlResolvedThroughAnEnvironmentPlaceholder() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("externalConfig", Map.of(
                "spring.datasource.url", "jdbc:postgresql://${DB_HOST}:5432/matriz_db")));
        environment.getPropertySources().addFirst(new MapPropertySource("systemEnvironment", Map.of(
                "DB_HOST", "db.example.com")));

        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(environment));
    }

    @Test
    void rejectsRemoteDatabaseUrlOverrideEvenWhenFinalDatasourceUrlIsLoopback() {
        MockEnvironment environment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        environment.setProperty("DB_URL", "jdbc:postgresql://db.example.com:5432/matriz_db");

        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(environment));
    }

    @Test
    void rejectsHikariPropertiesThatCouldRedirectTheConnection() {
        for (String propertyName : new String[] {
            "spring.datasource.hikari.jdbc-url",
            "spring.datasource.hikari.data-source-class-name",
            "spring.datasource.hikari.data-source-j-n-d-i",
            "spring.datasource.hikari.data-source-properties.serverName",
            "SPRING_DATASOURCE_HIKARI_JDBC_URL",
            "spring.datasource.tomcat.url",
            "spring.datasource.dbcp2.url",
            "spring.datasource.oracleucp.server-name"
        }) {
            MockEnvironment environment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "override", Map.of(propertyName, "jdbc:postgresql://remote-host:5432/matriz_db")));

            assertThrows(IllegalArgumentException.class,
                    () -> LocalDatabaseUrlGuard.validateAndPin(environment), propertyName);
        }
    }

    @Test
    void rejectsCustomDatasourceTypeAndNonPostgresDriver() {
        MockEnvironment typeEnvironment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        typeEnvironment.setProperty("spring.datasource.type", "com.example.CustomDataSource");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(typeEnvironment));

        MockEnvironment embeddedEnvironment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        embeddedEnvironment.setProperty("spring.datasource.embedded-database-connection", "h2");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(embeddedEnvironment));

        MockEnvironment driverEnvironment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        driverEnvironment.setProperty("spring.datasource.driver-class-name", "com.mysql.Driver");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(driverEnvironment));
    }

    @Test
    void rejectsMalformedUnsupportedAndNonLoopbackUrls() {
        for (String jdbcUrl : new String[] {
            "jdbc:postgresql://db.example.com:5432/matriz_db",
            "jdbc:mysql://localhost/matriz_db",
            "jdbc:postgresql:localhost/matriz_db",
            "jdbc:postgresql://localhost:bad-port/matriz_db",
            "jdbc:postgresql://localhost:65536/matriz_db",
            "jdbc:postgresql://localhost:0/matriz_db",
            "jdbc:postgresql://localhost:5432/matriz_db?ssl=true",
            "jdbc:postgresql://localhost:5432/matriz_db?socketFactory=com.example.RemoteSocketFactory",
            "jdbc:postgresql://localhost:5432,remote-host:5432/matriz_db",
            "jdbc:postgresql://user:password@localhost:5432/matriz_db",
            "jdbc:postgresql://localhost:5432/matriz_db#remote-host",
            "jdbc:postgresql://localhost:5432/invalid/database",
            "jdbc:postgresql://localhost:5432/%not-encoded"
        }) {
            MockEnvironment environment = environmentWithUrl(jdbcUrl);
            assertThrows(IllegalArgumentException.class,
                    () -> LocalDatabaseUrlGuard.validateAndPin(environment), jdbcUrl);
        }
    }

    @Test
    void rejectsAlternateDatasourceAndJsonOverrides() {
        MockEnvironment jndiEnvironment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        jndiEnvironment.setProperty("spring.datasource.jndi-name", "jdbc/remote");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(jndiEnvironment));

        MockEnvironment jsonEnvironment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        jsonEnvironment.setProperty("SPRING_APPLICATION_JSON", "{}");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPin(jsonEnvironment));
    }

    @Test
    void pinsValidatedUrlAboveOtherConfigurationSourcesAndDisablesFlyway() {
        MockEnvironment environment = environmentWithUrl("jdbc:postgresql://localhost:5432/matriz_db");
        environment.getPropertySources().addLast(new MapPropertySource(
                "laterExternalConfig", Map.of("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/matriz_db")));

        LocalDatabaseUrlGuard.validateAndPin(environment);

        assertEquals("jdbc:postgresql://localhost:5432/matriz_db", environment.getProperty("spring.datasource.url"));
        assertFalse(environment.getProperty("spring.flyway.enabled", Boolean.class, true));
    }

    @Test
    void permitsOnlyTheExactComposeDatabaseAndApplicationRoleAndPinsBoth() {
        MockEnvironment environment = composeEnvironment();

        assertEquals(COMPOSE_DATABASE_URL, LocalDatabaseUrlGuard.validateAndPinCompose(environment));
        assertEquals(COMPOSE_DATABASE_URL, environment.getProperty("spring.datasource.url"));
        assertEquals(COMPOSE_DATABASE_USERNAME, environment.getProperty("spring.datasource.username"));
        assertFalse(environment.getProperty("spring.flyway.enabled", Boolean.class, true));
    }

    @Test
    void rejectsComposeTargetsForOtherDatabasesHostsPortsAndRoles() {
        for (String jdbcUrl : new String[] {
            "jdbc:postgresql://127.0.0.1:5433/matriz_db",
            "jdbc:postgresql://127.0.0.1:5433/matriz_test",
            "jdbc:postgresql://127.0.0.1:5433/other",
            "jdbc:postgresql://localhost:5433/matriz_compose",
            "jdbc:postgresql://127.0.0.1:5432/matriz_compose",
            "jdbc:postgresql://database:5432/matriz_compose"
        }) {
            MockEnvironment environment = composeEnvironment();
            environment.setProperty("spring.datasource.url", jdbcUrl);

            assertThrows(IllegalArgumentException.class,
                    () -> LocalDatabaseUrlGuard.validateAndPinCompose(environment), jdbcUrl);
        }

        MockEnvironment wrongRole = composeEnvironment();
        wrongRole.setProperty("spring.datasource.username", "matriz_compose_admin");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPinCompose(wrongRole));
    }

    @Test
    void rejectsMismatchedExplicitDatasourceUrlOverrides() {
        for (String propertyName : new String[] {"DB_URL", "SPRING_DATASOURCE_URL"}) {
            for (String jdbcUrl : new String[] {
                "jdbc:postgresql://127.0.0.1:5433/matriz_db",
                "jdbc:postgresql://127.0.0.1:5433/matriz_test",
                "jdbc:postgresql://db.example.com:5432/matriz_compose"
            }) {
                MockEnvironment environment = composeEnvironment();
                environment.setProperty(propertyName, jdbcUrl);

                assertThrows(IllegalArgumentException.class,
                        () -> LocalDatabaseUrlGuard.validateAndPinCompose(environment),
                        propertyName);
            }
        }
    }

    @Test
    void rejectsMismatchedExplicitDatasourceUsernameOverrides() {
        for (String propertyName : new String[] {"DB_USERNAME", "SPRING_DATASOURCE_USERNAME"}) {
            MockEnvironment environment = composeEnvironment();
            environment.setProperty(propertyName, "matriz_compose_admin");

            assertThrows(IllegalArgumentException.class,
                    () -> LocalDatabaseUrlGuard.validateAndPinCompose(environment),
                    propertyName);
        }
    }

    @Test
    void composeValidationRetainsDatasourceRedirectionProtections() {
        MockEnvironment hikariEnvironment = composeEnvironment();
        hikariEnvironment.getPropertySources().addFirst(new MapPropertySource(
                "override", Map.of("spring.datasource.hikari.jdbc-url",
                        "jdbc:postgresql://127.0.0.1:5433/matriz_compose")));
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPinCompose(hikariEnvironment));

        MockEnvironment jsonEnvironment = composeEnvironment();
        jsonEnvironment.setProperty("SPRING_APPLICATION_JSON", "{}");
        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPinCompose(jsonEnvironment));
    }

    @Test
    void rejectsResolvedUsernameOverrideEvenWhenComposeProfileIsNotActive() {
        MockEnvironment environment = composeEnvironment();
        environment.setProperty("spring.datasource.username", "another_role");

        assertThrows(IllegalArgumentException.class,
                () -> LocalDatabaseUrlGuard.validateAndPinCompose(environment));
    }

    private static MockEnvironment environmentWithUrl(String jdbcUrl) {
        return new MockEnvironment().withProperty("spring.datasource.url", jdbcUrl);
    }

    private static MockEnvironment composeEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", COMPOSE_DATABASE_URL)
                .withProperty("spring.datasource.username", COMPOSE_DATABASE_USERNAME);
    }
}
