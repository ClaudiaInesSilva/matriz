package com.matriz.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final Environment environment;
    private final boolean autoLoginEnabled;

    public AuthenticationController(
            AuthenticationService authenticationService,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            Environment environment,
            @Value("${matriz.demo-accounts.auto-login-enabled:false}") boolean autoLoginEnabled) {
        this.authenticationService = authenticationService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.environment = environment;
        this.autoLoginEnabled = autoLoginEnabled;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(
            @RequestAttribute("_csrf") CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        final Authentication authentication;
        try {
            authentication = authenticationService.authenticate(request.username(), request.password());
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        return ResponseEntity.ok(authenticationService.profile(authentication));
    }

    @PostMapping("/demo-login")
    public ResponseEntity<AuthenticatedUserResponse> demoLogin(
            @Valid @RequestBody DemoLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (!autoLoginEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!environment.acceptsProfiles(Profiles.of("render"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Authentication authentication;
        try {
            authentication = authenticationService.authenticateDemoAccount(request.username());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        return ResponseEntity.ok(authenticationService.profile(authentication));
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(@AuthenticationPrincipal MatrizUserPrincipal principal) {
        return AuthenticatedUserResponse.from(principal);
    }
}
