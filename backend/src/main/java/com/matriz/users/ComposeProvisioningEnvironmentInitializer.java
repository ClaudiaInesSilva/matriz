package com.matriz.users;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Profiles;

/** Validates the explicit Compose provisioning target before Spring creates any datasource. */
public final class ComposeProvisioningEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (!applicationContext.getEnvironment().acceptsProfiles(Profiles.of("compose-provisioning"))) {
            throw new IllegalStateException("Compose demo provisioning requires the compose-provisioning profile");
        }
        LocalDatabaseUrlGuard.validateAndPinCompose(applicationContext.getEnvironment());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
