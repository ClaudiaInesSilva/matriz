package com.matriz.users;

import java.io.Console;
import java.util.Arrays;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDemoAccountProvisioner {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LocalDemoAccountProvisioner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void provision(Console console) {
        if (console == null) {
            throw new IllegalStateException("Run the local provisioning command in an interactive terminal");
        }

        for (String username : DemoAccountPolicy.RESERVED_USERNAMES) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Expected local demo account is missing: " + username));
            char[] password = console.readPassword("Set local-only password for %s: ", username);
            char[] confirmation = console.readPassword("Confirm local-only password for %s: ", username);
            try {
                if (password == null || password.length == 0 || !Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("Passwords must be non-empty and match");
                }
                user.replacePasswordHash(passwordEncoder.encode(new String(password)));
                userRepository.save(user);
            } finally {
                if (password != null) {
                    Arrays.fill(password, '\0');
                }
                if (confirmation != null) {
                    Arrays.fill(confirmation, '\0');
                }
            }
        }
    }
}
