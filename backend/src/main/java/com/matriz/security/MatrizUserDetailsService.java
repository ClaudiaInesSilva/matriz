package com.matriz.security;

import com.matriz.users.DemoAccountPolicy;
import com.matriz.users.PasswordHashPolicy;
import com.matriz.users.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MatrizUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final Environment environment;
    private final boolean demoAuthenticationEnabled;

    public MatrizUserDetailsService(
            UserRepository userRepository,
            Environment environment,
            @Value("${matriz.demo-accounts.authentication-enabled:false}") boolean demoAuthenticationEnabled) {
        this.userRepository = userRepository;
        this.environment = environment;
        this.demoAuthenticationEnabled = demoAuthenticationEnabled;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (DemoAccountPolicy.isReservedUsername(username)
                && (!demoAuthenticationEnabled || !environment.acceptsProfiles(Profiles.of("local", "compose", "render")))) {
            throw unavailableAccount();
        }

        var user = userRepository.findByUsername(username).orElseThrow(this::unavailableAccount);
        if (!PasswordHashPolicy.isUsableBcryptHash(user.getPasswordHash())) {
            throw unavailableAccount();
        }

        return new MatrizUserPrincipal(
                user.getId(), user.getUsername(), user.getPasswordHash(), user.getDisplayName());
    }

    public MatrizUserPrincipal loadDemoUserByUsername(String username) throws UsernameNotFoundException {
        if (!DemoAccountPolicy.isReservedUsername(username)) {
            throw unavailableAccount();
        }
        if (!demoAuthenticationEnabled || !environment.acceptsProfiles(Profiles.of("local", "compose", "render"))) {
            throw unavailableAccount();
        }
        var user = userRepository.findByUsername(username).orElseThrow(this::unavailableAccount);
        // Skip BCrypt hash check: demo login is passwordless.
        return new MatrizUserPrincipal(
                user.getId(), user.getUsername(), user.getPasswordHash(), user.getDisplayName());
    }

    private UsernameNotFoundException unavailableAccount() {
        return new UsernameNotFoundException("Invalid username or password");
    }
}
