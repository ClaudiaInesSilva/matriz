package com.matriz.users;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Separate, explicit entry point for one-off Compose demo credential provisioning. */
public final class ComposeDemoAccountProvisioningApplication {
    private ComposeDemoAccountProvisioningApplication() {}

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(LocalProvisioningConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles(provisioningProfiles())
                .initializers(environmentInitializer())
                .run(args)) {
            context.getBean(LocalDemoAccountProvisioner.class).provision(System.console());
        }
    }

    static String[] provisioningProfiles() {
        return new String[] {"local", "local-provisioning", "compose-provisioning"};
    }

    static ComposeProvisioningEnvironmentInitializer environmentInitializer() {
        return new ComposeProvisioningEnvironmentInitializer();
    }
}
