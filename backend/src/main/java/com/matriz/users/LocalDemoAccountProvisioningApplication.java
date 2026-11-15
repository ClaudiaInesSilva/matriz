package com.matriz.users;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Separate, explicit entry point for one-off local demo credential provisioning. */
public final class LocalDemoAccountProvisioningApplication {
    private LocalDemoAccountProvisioningApplication() {}

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(LocalProvisioningConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("local")
                .initializers(new LocalProvisioningEnvironmentInitializer())
                .run(args)) {
            context.getBean(LocalDemoAccountProvisioner.class).provision(System.console());
        }
    }
}
