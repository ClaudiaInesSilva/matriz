package com.matriz.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.WebApplicationType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ComposeProvisioningEnvironmentInitializerTest {
    private static final AtomicBoolean DATA_SOURCE_CREATED = new AtomicBoolean();

    @Test
    void rejectsInvalidComposeDatabaseBeforeDatasourceBeanCreation() {
        DATA_SOURCE_CREATED.set(false);

        assertThrows(RuntimeException.class, () -> new SpringApplicationBuilder(DataSourceProbeConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("local", "local-provisioning", "compose-provisioning")
                .properties(
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/matriz_test",
                        "spring.datasource.username=matriz_compose_app")
                .initializers(new ComposeProvisioningEnvironmentInitializer())
                .run());

        assertFalse(DATA_SOURCE_CREATED.get());
    }

    @Test
    void composeModeSelectionDoesNotChangeLocalDatasourceValidation() {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/matriz_db")
                .withProperty("spring.datasource.username", "matriz_app")
                .withProperty("spring.profiles.active", "local");

        org.springframework.core.env.ConfigurableEnvironment configurableEnvironment = environment;
        assertFalse(configurableEnvironment.acceptsProfiles(
                org.springframework.core.env.Profiles.of("compose-provisioning")));
        org.junit.jupiter.api.Assertions.assertEquals(
                "jdbc:postgresql://localhost:5432/matriz_db",
                LocalProvisioningConfiguration.validateDatasourceConfiguration(configurableEnvironment));
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @Configuration(proxyBeanMethods = false)
    @Profile("local-provisioning")
    static class DataSourceProbeConfiguration {
        @Bean
        DataSource dataSource() {
            DATA_SOURCE_CREATED.set(true);
            return org.mockito.Mockito.mock(DataSource.class);
        }
    }
}
