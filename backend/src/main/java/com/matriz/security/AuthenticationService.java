package com.matriz.security;

import com.matriz.users.DemoAccountPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final MatrizUserDetailsService matrizUserDetailsService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            MatrizUserDetailsService matrizUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.matrizUserDetailsService = matrizUserDetailsService;
    }

    public Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
    }

    public Authentication authenticateDemoAccount(String username) {
        if (!DemoAccountPolicy.isReservedUsername(username)) {
            throw new IllegalArgumentException("Invalid demo username");
        }

        MatrizUserPrincipal principal = (MatrizUserPrincipal) matrizUserDetailsService.loadUserByUsername(username);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    public AuthenticatedUserResponse profile(Authentication authentication) {
        return AuthenticatedUserResponse.from((MatrizUserPrincipal) authentication.getPrincipal());
    }
}
