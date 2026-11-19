package com.matriz.users;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class ComposeDemoAccountProvisioningApplicationTest {
    @Test
    void explicitlySelectsComposeProvisioningProfilesAndInitializer() {
        assertArrayEquals(
                new String[] {"local", "local-provisioning", "compose-provisioning"},
                ComposeDemoAccountProvisioningApplication.provisioningProfiles());
        assertInstanceOf(
                ComposeProvisioningEnvironmentInitializer.class,
                ComposeDemoAccountProvisioningApplication.environmentInitializer());
    }
}
