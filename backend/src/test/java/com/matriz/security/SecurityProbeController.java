package com.matriz.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

/** Test-only routes for exercising the security filter chain before application endpoints exist. */
@RestController
class SecurityProbeController {
    @GetMapping("/api/probe/csrf")
    String csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return token.getToken();
    }

    @GetMapping("/api/probe/private")
    String protectedRoute() {
        return "protected route";
    }

    @PostMapping("/api/probe/state-change")
    String stateChange() {
        return "state change accepted";
    }

    @GetMapping("/api/probe/throw-error")
    String throwError() {
        throw new RuntimeException("Intentional error for testing ERROR dispatcher");
    }
}
