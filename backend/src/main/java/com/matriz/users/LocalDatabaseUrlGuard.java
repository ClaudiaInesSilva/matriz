package com.matriz.users;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/** Validates and pins the datasource configuration used by the local provisioning context. */
public final class LocalDatabaseUrlGuard {
    private static final String LOCKED_CONFIGURATION = "matrizLocalProvisioningConfiguration";
    private static final Pattern LOOPBACK_AUTHORITY = Pattern.compile(
            "(?i)(localhost|127\\.0\\.0\\.1|\\[::1\\])(?::([0-9]{1,5}))?");
    private static final Pattern DATABASE_PATH = Pattern.compile("/[A-Za-z0-9._~-]+");
    private static final String[] ALTERNATE_POOL_PREFIXES = {
        "SPRING_DATASOURCE_HIKARI",
        "SPRING_DATASOURCE_TOMCAT",
        "SPRING_DATASOURCE_DBCP2",
        "SPRING_DATASOURCE_ORACLEUCP"
    };

    private LocalDatabaseUrlGuard() {}

    /**
     * Resolves the final datasource URL from Spring's prepared environment, validates it,
     * then pins that exact value before context refresh creates the datasource.
     */
    public static String validateAndPin(ConfigurableEnvironment environment) {
        if (environment.getProperty("SPRING_APPLICATION_JSON") != null) {
            throw new IllegalArgumentException("Demo account provisioning does not accept SPRING_APPLICATION_JSON overrides");
        }
        if (environment.getProperty("spring.datasource.jndi-name") != null) {
            throw new IllegalArgumentException("Demo account provisioning does not support a JNDI datasource");
        }
        if (environment.getProperty("spring.datasource.type") != null) {
            throw new IllegalArgumentException("Demo account provisioning does not support a custom datasource type");
        }
        if (environment.getProperty("spring.datasource.embedded-database-connection") != null) {
            throw new IllegalArgumentException("Demo account provisioning does not support embedded datasource selection");
        }
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        if (driverClassName != null && !driverClassName.equals("org.postgresql.Driver")) {
            throw new IllegalArgumentException("Demo account provisioning requires the PostgreSQL JDBC driver");
        }
        rejectHikariOverrides(environment);

        for (String propertyName : new String[] {"DB_URL", "SPRING_DATASOURCE_URL"}) {
            String configuredOverride = environment.getProperty(propertyName);
            if (configuredOverride != null && !configuredOverride.isBlank()) {
                requireLoopbackPostgresUrl(configuredOverride);
            }
        }

        String resolvedUrl = environment.getProperty("spring.datasource.url");
        requireLoopbackPostgresUrl(resolvedUrl);

        environment.getPropertySources().addFirst(new MapPropertySource(LOCKED_CONFIGURATION, Map.of(
                "spring.datasource.url", resolvedUrl,
                "spring.datasource.driver-class-name", "org.postgresql.Driver",
                "spring.flyway.enabled", "false")));
        return resolvedUrl;
    }

    private static void rejectHikariOverrides(ConfigurableEnvironment environment) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerableSource) {
                for (String propertyName : enumerableSource.getPropertyNames()) {
                    String normalized = propertyName.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
                    for (String prefix : ALTERNATE_POOL_PREFIXES) {
                        if (normalized.equals(prefix) || normalized.startsWith(prefix + "_")) {
                            throw new IllegalArgumentException(
                                    "Demo account provisioning does not accept alternate datasource pool overrides");
                        }
                    }
                }
            }
        }
    }

    private static void requireLoopbackPostgresUrl(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Demo account provisioning requires a valid local PostgreSQL URL");
        }

        try {
            URI uri = new URI(jdbcUrl.substring("jdbc:".length()));
            if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw invalidJdbcUrl();
            }

            Matcher authority = LOOPBACK_AUTHORITY.matcher(uri.getRawAuthority() == null ? "" : uri.getRawAuthority());
            if (!authority.matches()) {
                throw invalidJdbcUrl();
            }

            String portText = authority.group(2);
            if (portText != null) {
                int port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    throw invalidJdbcUrl();
                }
            }

            String rawPath = uri.getRawPath();
            if (rawPath != null && !rawPath.isEmpty() && !rawPath.equals("/")
                    && !DATABASE_PATH.matcher(rawPath).matches()) {
                throw invalidJdbcUrl();
            }
        } catch (URISyntaxException exception) {
            throw invalidJdbcUrl();
        } catch (NumberFormatException exception) {
            throw invalidJdbcUrl();
        }
    }

    private static IllegalArgumentException invalidJdbcUrl() {
        return new IllegalArgumentException(
                "Provisioning accepts only a PostgreSQL URL with one loopback host, an optional valid port, and an optional database name");
    }
}
