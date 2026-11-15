package com.matriz.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashPolicyTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void acceptsBcryptHashesOnly() {
        assertTrue(PasswordHashPolicy.isUsableBcryptHash(encoder.encode("fictional-local-password")));
        assertFalse(PasswordHashPolicy.isUsableBcryptHash(null));
        assertFalse(PasswordHashPolicy.isUsableBcryptHash("!DISABLED!"));
        assertFalse(PasswordHashPolicy.isUsableBcryptHash("not-a-bcrypt-hash"));
    }
}
