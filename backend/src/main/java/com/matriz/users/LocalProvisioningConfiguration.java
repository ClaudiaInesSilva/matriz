package com.matriz.users;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Minimal Spring context for the one-off local demo-account provisioning command. */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = User.class)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
@Import(LocalDemoAccountProvisioner.class)
public class LocalProvisioningConfiguration {
    @Bean(destroyMethod = "close")
    @Profile("local")
    DataSource localProvisioningDataSource(ConfigurableEnvironment environment) {
        String validatedUrl = LocalDatabaseUrlGuard.validateAndPin(environment);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(validatedUrl);
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setUsername(environment.getProperty("spring.datasource.username"));
        hikariConfig.setPassword(environment.getProperty("spring.datasource.password"));
        return new HikariDataSource(hikariConfig);
    }
}
