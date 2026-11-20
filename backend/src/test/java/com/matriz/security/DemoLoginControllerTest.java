package com.matriz.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.users.User;
import com.matriz.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

@WebMvcTest(controllers = AuthenticationController.class)
@Import({
        SecurityConfiguration.class,
        AuthenticationService.class,
        MatrizUserDetailsService.class,
        DemoLoginControllerTest.RepositoryConfiguration.class
})
@ActiveProfiles("render")
@TestPropertySource(properties = "matriz.demo-accounts.auto-login-enabled=true")
class DemoLoginControllerTest {
    private static final String DEMO_LOGIN_URL = "/api/auth/demo-login";
    private static final String CSRF_URL = "/api/auth/csrf";
    private static final String PROFILE_URL = "/api/auth/me";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CsrfTokenRepository csrfTokenRepository;

    private UUID alexId;

    @BeforeEach
    void setUpUsers() {
        alexId = UUID.fromString("60000000-0000-4000-8000-000000000001");
        when(userRepository.findByUsername("demo.alex"))
                .thenReturn(Optional.of(new User(alexId, "demo.alex", "!DISABLED!", "Alex")));
    }

    @Test
    void demoAlexLoginSucceedsWithPropertyAndProfileEnabled() throws Exception {
        CsrfSession initial = getCsrf();
        String initialSessionId = initial.session().getId();

        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(alexId.toString()))
                .andExpect(jsonPath("$.username").value("demo.alex"))
                .andExpect(jsonPath("$.displayName").value("Alex"));

        assertThat(initial.session().getId()).isNotEqualTo(initialSessionId);
    }

    @Test
    void demoSamLoginSucceedsWithPropertyAndProfileEnabled() throws Exception {
        UUID samId = UUID.fromString("60000000-0000-4000-8000-000000000002");
        when(userRepository.findByUsername("demo.sam"))
                .thenReturn(Optional.of(new User(samId, "demo.sam", "!DISABLED!", "Sam")));

        CsrfSession initial = getCsrf();

        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.sam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(samId.toString()))
                .andExpect(jsonPath("$.username").value("demo.sam"))
                .andExpect(jsonPath("$.displayName").value("Sam"));
    }

    @Test
    void arbitraryUsernameIsRejectedWithBadRequest() throws Exception {
        CsrfSession csrf = getCsrf();
        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"some.other.user\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullUsernameIsRejectedWithBadRequest() throws Exception {
        CsrfSession csrf = getCsrf();
        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankUsernameIsRejectedWithBadRequest() throws Exception {
        CsrfSession csrf = getCsrf();
        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Nested
    @TestPropertySource(properties = "matriz.demo-accounts.auto-login-enabled=false")
    class PropertyDisabled {
        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepository;

        @Autowired
        PasswordEncoder passwordEncoder;

        private final UUID alexId = UUID.fromString("60000000-0000-4000-8000-000000000001");

        @BeforeEach
        void setUp() {
            when(userRepository.findByUsername("demo.alex"))
                    .thenReturn(Optional.of(new User(alexId, "demo.alex", "!DISABLED!", "Alex")));
        }

        @Test
        void propertyDisabledRejectsWithForbidden() throws Exception {
            CsrfSession csrf = getCsrf();
            mockMvc.perform(post(DEMO_LOGIN_URL)
                            .session(csrf.session())
                            .header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"demo.alex\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @ActiveProfiles(profiles = "local", inheritProfiles = false)
    @TestPropertySource(properties = "matriz.demo-accounts.auto-login-enabled=true")
    class ProfileMismatch {
        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepository;

        @Autowired
        PasswordEncoder passwordEncoder;

        private final UUID alexId = UUID.fromString("60000000-0000-4000-8000-000000000001");

        @BeforeEach
        void setUp() {
            when(userRepository.findByUsername("demo.alex"))
                    .thenReturn(Optional.of(new User(alexId, "demo.alex", "!DISABLED!", "Alex")));
        }

        @Test
        void profileMismatchRejectsWithForbidden() throws Exception {
            CsrfSession csrf = getCsrf();
            mockMvc.perform(post(DEMO_LOGIN_URL)
                            .session(csrf.session())
                            .header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"demo.alex\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCsrfTokenIsRejected() throws Exception {
        CsrfSession csrf = getCsrf();
        String modifiedToken = csrf.token().substring(0, csrf.token().length() - 1)
                + (csrf.token().endsWith("A") ? "B" : "A");
        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", modifiedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionRotatesAfterDemoLogin() throws Exception {
        CsrfSession initial = getCsrf();
        String initialSessionId = initial.session().getId();

        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\"}"))
                .andExpect(status().isOk());

        assertThat(initial.session().getId()).isNotEqualTo(initialSessionId);
    }

    @Test
    void csrfTokenRegeneratesAfterDemoLogin() throws Exception {
        CsrfSession initial = getCsrf();

        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\"}"))
                .andExpect(status().isOk());

        CsrfSession renewed = getCsrf(initial.session());
        assertThat(renewed.token()).isNotEqualTo(initial.token());
    }

    @Test
    void profileEndpointWorksAfterDemoLogin() throws Exception {
        CsrfSession initial = getCsrf();

        mockMvc.perform(post(DEMO_LOGIN_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROFILE_URL).session(initial.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(alexId.toString()))
                .andExpect(jsonPath("$.username").value("demo.alex"));
    }

    private CsrfSession getCsrf() throws Exception {
        return getCsrf(null);
    }

    private CsrfSession getCsrf(MockHttpSession existingSession) throws Exception {
        var request = get(CSRF_URL);
        if (existingSession != null) {
            request.session(existingSession);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        java.util.regex.Matcher token = java.util.regex.Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        return new CsrfSession(token.group(1), (MockHttpSession) result.getRequest().getSession(false));
    }

    private record CsrfSession(String token, MockHttpSession session) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class RepositoryConfiguration {
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        @Primary
        SessionAuthenticationStrategy mockMvcSessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
            SessionAuthenticationStrategy containerStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                    new ChangeSessionIdAuthenticationStrategy(),
                    new CsrfAuthenticationStrategy(csrfTokenRepository)));
            return (authentication, request, response) -> {
                if (request instanceof org.springframework.mock.web.MockHttpServletRequest mockRequest) {
                    mockRequest.setRequestedSessionIdValid(true);
                }
                HttpServletRequest containerLikeRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public boolean isRequestedSessionIdValid() {
                        return true;
                    }

                    @Override
                    public String changeSessionId() {
                        return ((MockHttpSession) getSession()).changeSessionId();
                    }
                };
                containerStrategy.onAuthentication(authentication, containerLikeRequest, response);
            };
        }
    }
}
