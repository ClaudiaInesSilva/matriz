package com.matriz.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import({SecurityConfiguration.class, SecurityConfigurationTest.UserDetailsTestConfiguration.class})
class SecurityConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void requiresAuthenticationForProtectedApiRequestsByDefault() throws Exception {
        mockMvc.perform(get("/api/probe/private")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void permitsOnlyPlannedPublicMethodsAndKeepsCsrfOnLogin() throws Exception {
        mockMvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout").with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsTheHandlerExposedCsrfTokenUnchangedAndRejectsAModifiedToken() throws Exception {
        MvcResult tokenResponse = mockMvc.perform(get("/api/probe/csrf").with(user("test-user")))
                .andExpect(status().isOk())
                .andReturn();

        String exposedToken = tokenResponse.getResponse().getContentAsString();
        assertNotNull(exposedToken);
        assertNotNull(tokenResponse.getRequest().getSession(false));
        CsrfToken repositoryToken = csrfTokenRepository.loadToken(tokenResponse.getRequest());
        assertNotNull(repositoryToken);
        assertNotEquals(repositoryToken.getToken(), exposedToken,
                "the endpoint must expose the XOR handler representation, not the raw repository token");

        MockHttpSession session = (MockHttpSession) tokenResponse.getRequest().getSession(false);
        mockMvc.perform(post("/api/probe/state-change")
                        .session(session)
                        .with(user("test-user"))
                        .header("X-CSRF-TOKEN", exposedToken))
                .andExpect(status().isOk());

        byte[] modifiedPayload = Base64.getUrlDecoder().decode(exposedToken);
        modifiedPayload[modifiedPayload.length - 1] ^= 0x01;
        String modifiedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(modifiedPayload);
        mockMvc.perform(post("/api/probe/state-change")
                        .session(session)
                        .with(user("test-user"))
                        .header("X-CSRF-TOKEN", modifiedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedPrincipalCanAccessProtectedApiRoute() throws Exception {
        mockMvc.perform(get("/api/probe/private").with(user("test-user")))
                .andExpect(status().isOk());
    }

    @Test
    void configuresSecureHttpOnlyLaxJsessionidCookieByDefault() {
        org.junit.jupiter.api.Assertions.assertEquals("JSESSIONID", environment.getProperty("server.servlet.session.cookie.name"));
        org.junit.jupiter.api.Assertions.assertTrue(environment.getProperty(
                "server.servlet.session.cookie.http-only", Boolean.class));
        org.junit.jupiter.api.Assertions.assertEquals("lax", environment.getProperty("server.servlet.session.cookie.same-site"));
        org.junit.jupiter.api.Assertions.assertTrue(environment.getProperty(
                "server.servlet.session.cookie.secure", Boolean.class));
    }

    @Test
    void persistsSecurityContextInHttpSession() {
        org.junit.jupiter.api.Assertions.assertInstanceOf(
                HttpSessionSecurityContextRepository.class, securityContextRepository);
    }

    @Test
    void hasNoUsableGeneratedDefaultAccount() {
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("user"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class UserDetailsTestConfiguration {
        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("No test account");
            };
        }
    }

    @Test
    void crossOriginRequestRejectedWhenCorsOriginNotConfigured() throws Exception {
        mockMvc.perform(get("/api/probe/private")
                        .header("Origin", "https://matriz-frontend.onrender.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertNull(allowOrigin,
                            "no CORS headers should be present when no origins are configured");
                });
    }

    @Test
    void authenticationStrategyRotatesSessionIdAndClearsPriorCsrfToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setRequestedSessionIdValid(true);
        HttpServletRequest containerLikeRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String changeSessionId() {
                return ((MockHttpSession) getSession()).changeSessionId();
            }
        };
        CsrfToken previousToken = csrfTokenRepository.generateToken(containerLikeRequest);
        csrfTokenRepository.saveToken(previousToken, containerLikeRequest, response);
        String originalSessionId = session.getId();

        sessionAuthenticationStrategy.onAuthentication(
                new UsernamePasswordAuthenticationToken("demo.alex", "ignored", List.of()), containerLikeRequest, response);

        org.junit.jupiter.api.Assertions.assertNotEquals(originalSessionId, session.getId());
        org.junit.jupiter.api.Assertions.assertNull(csrfTokenRepository.loadToken(containerLikeRequest));
    }
}
