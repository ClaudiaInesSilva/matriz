package com.matriz.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.matriz.users.User;
import com.matriz.users.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MatrizUserDetailsServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void acceptsNonDemoAccountWithValidBcryptHashWhenDemoAuthenticationIsDisabled() {
        String hash = encoder.encode("fictional-test-password");
        when(users.findByUsername("employee.one")).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "employee.one", hash, "Employee One")));
        MatrizUserDetailsService service = new MatrizUserDetailsService(users, new MockEnvironment(), false);

        MatrizUserPrincipal principal = (MatrizUserPrincipal) service.loadUserByUsername("employee.one");

        assertThat(principal.getUsername()).isEqualTo("employee.one");
        assertThat(encoder.matches("fictional-test-password", principal.getPassword())).isTrue();
    }

    @Test
    void reservedDemoAccountIsDisabledWithoutLocalProfileAndExplicitFlag() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "local");
        MatrizUserDetailsService disabled = new MatrizUserDetailsService(users, environment, false);
        assertThrows(UsernameNotFoundException.class, () -> disabled.loadUserByUsername("demo.alex"));

        MatrizUserDetailsService nonLocal = new MatrizUserDetailsService(
                users, new MockEnvironment(), true);
        assertThrows(UsernameNotFoundException.class, () -> nonLocal.loadUserByUsername("demo.alex"));
    }

    @Test
    void localFlagAllowsOnlyTheExactReservedDemoUsernameSet() {
        MockEnvironment local = new MockEnvironment();
        local.setActiveProfiles("local");
        String hash = encoder.encode("fictional-local-demo-password");
        when(users.findByUsername("demo.alex")).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "demo.alex", hash, "Alex Demo")));
        MatrizUserDetailsService enabled = new MatrizUserDetailsService(users, local, true);

        assertThat(enabled.loadUserByUsername("demo.alex").getUsername()).isEqualTo("demo.alex");
        assertThrows(UsernameNotFoundException.class, () -> enabled.loadUserByUsername("demo.other"));
    }

    @Test
    void nullDisabledAndMalformedHashesCannotAuthenticate() {
        MockEnvironment environment = new MockEnvironment();
        when(users.findByUsername("null.hash")).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "null.hash", null, "Null Hash")));
        when(users.findByUsername("disabled.hash")).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "disabled.hash", "!DISABLED!", "Disabled Hash")));
        when(users.findByUsername("malformed.hash")).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "malformed.hash", "not-a-bcrypt-hash", "Malformed Hash")));
        MatrizUserDetailsService service = new MatrizUserDetailsService(users, environment, false);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("null.hash"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("disabled.hash"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("malformed.hash"));
    }
}
