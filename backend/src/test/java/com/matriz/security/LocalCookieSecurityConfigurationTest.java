package com.matriz.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/** Loads only local-profile configuration, without bootstrapping application auto-configuration or a datasource. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("local")
class LocalCookieSecurityConfigurationTest {
    @Autowired
    private Environment environment;

    @Test
    void disablesSecureCookieForLocalHttpDevelopmentOnly() {
        assertFalse(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class));
    }

}
