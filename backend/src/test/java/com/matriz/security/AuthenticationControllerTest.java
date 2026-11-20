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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

@WebMvcTest(controllers = AuthenticationController.class)
@Import({
        SecurityConfiguration.class,
        AuthenticationService.class,
        MatrizUserDetailsService.class,
        AuthenticationControllerTest.RepositoryConfiguration.class
})
class AuthenticationControllerTest {
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String CSRF_URL = "/api/auth/csrf";
    private static final String PROFILE_URL = "/api/auth/me";
    private static final String LOGOUT_URL = "/api/auth/logout";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CsrfTokenRepository csrfTokenRepository;

    private UUID memberId;
    private UUID otherId;

    @BeforeEach
    void setUpUsers() {
        memberId = UUID.fromString("50000000-0000-4000-8000-000000000001");
        otherId = UUID.fromString("50000000-0000-4000-8000-000000000002");
        when(userRepository.findByUsername("member.one"))
                .thenReturn(Optional.of(new User(memberId, "member.one", passwordEncoder.encode("fictional-test-password"), "Member One")));
        when(userRepository.findByUsername("member.two"))
                .thenReturn(Optional.of(new User(otherId, "member.two", passwordEncoder.encode("another-test-password"), "Member Two")));
    }

    @Test
    void csrfEndpointReturnsTheHandlerExposedTokenAndHeaderName() throws Exception {
        MvcResult result = mockMvc.perform(get(CSRF_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void validLoginRotatesSessionPersistsAcrossRequestsAndInvalidatesOldCsrfToken() throws Exception {
        CsrfSession initial = getCsrf();
        String initialSessionId = initial.session().getId();

        mockMvc.perform(post(LOGIN_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"member.one","password":"fictional-test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(memberId.toString()))
                .andExpect(jsonPath("$.username").value("member.one"))
                .andExpect(jsonPath("$.displayName").value("Member One"));

        assertThat(initial.session().getId()).isNotEqualTo(initialSessionId);
        mockMvc.perform(get(PROFILE_URL).session(initial.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(memberId.toString()))
                .andExpect(jsonPath("$.username").value("member.one"));

        mockMvc.perform(post(LOGOUT_URL)
                        .session(initial.session())
                        .header("X-CSRF-TOKEN", initial.token()))
                .andExpect(status().isForbidden());

        CsrfSession renewed = getCsrf(initial.session());
        assertThat(renewed.token()).isNotEqualTo(initial.token());
    }

    @Test
    void invalidCredentialsAndUnauthenticatedProfileAreRejected() throws Exception {
        CsrfSession csrf = getCsrf();
        mockMvc.perform(post(LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"member.one","password":"incorrect"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(PROFILE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void profileIdentifiesEachAuthenticatedUserFromTheSecurityContext() throws Exception {
        assertLoginAndProfile("member.one", "fictional-test-password", memberId, "Member One");
        assertLoginAndProfile("member.two", "another-test-password", otherId, "Member Two");
    }

    @Test
    void logoutFilterInvalidatesSessionAndSupportsFreshCsrfTokenAfterLogout() throws Exception {
        mockMvc.perform(post(LOGOUT_URL).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());

        CsrfSession loginCsrf = getCsrf();
        mockMvc.perform(post(LOGIN_URL)
                        .session(loginCsrf.session())
                        .header("X-CSRF-TOKEN", loginCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"member.one","password":"fictional-test-password"}
                                """))
                .andExpect(status().isOk());

        CsrfSession beforeLogout = getCsrf(loginCsrf.session());
        mockMvc.perform(post(LOGOUT_URL)
                        .session(loginCsrf.session())
                        .header("X-CSRF-TOKEN", beforeLogout.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PROFILE_URL).session(loginCsrf.session()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(LOGOUT_URL)
                        .header("X-CSRF-TOKEN", beforeLogout.token()))
                .andExpect(status().isForbidden());

        CsrfSession afterLogout = getCsrf();
        assertThat(afterLogout.token()).isNotEqualTo(beforeLogout.token());
        mockMvc.perform(post(LOGOUT_URL)
                        .session(afterLogout.session())
                        .header("X-CSRF-TOKEN", afterLogout.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedLogoutDoesNotRunLogoutHandlersOrInvalidateItsSession() throws Exception {
        CsrfSession anonymous = getCsrf();
        MockHttpServletRequest initialRepositoryRequest = new MockHttpServletRequest();
        initialRepositoryRequest.setSession(anonymous.session());
        CsrfToken storedToken = csrfTokenRepository.loadToken(initialRepositoryRequest);
        assertThat(storedToken).isNotNull();

        mockMvc.perform(post(LOGOUT_URL)
                        .session(anonymous.session())
                        .header("X-CSRF-TOKEN", anonymous.token()))
                .andExpect(status().isUnauthorized());

        MockHttpServletRequest afterRequest = new MockHttpServletRequest();
        afterRequest.setSession(anonymous.session());
        assertThat(csrfTokenRepository.loadToken(afterRequest)).isSameAs(storedToken);
    }

    @Test
    void modifiedCsrfTokenIsRejectedForLogin() throws Exception {
        CsrfSession csrf = getCsrf();
        String modifiedToken = csrf.token().substring(0, csrf.token().length() - 1)
                + (csrf.token().endsWith("A") ? "B" : "A");
        mockMvc.perform(post(LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", modifiedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"member.one","password":"fictional-test-password"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void demoAccountWithDisabledHashCannotLoginNormally() throws Exception {
        UUID demoAlexId = UUID.fromString("60000000-0000-4000-8000-000000000001");
        when(userRepository.findByUsername("demo.alex"))
                .thenReturn(Optional.of(new User(demoAlexId, "demo.alex", "!DISABLED!", "Alex Demo")));

        CsrfSession csrf = getCsrf();
        mockMvc.perform(post(LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo.alex","password":"any-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void assertLoginAndProfile(String username, String password, UUID userId, String displayName) throws Exception {
        CsrfSession csrf = getCsrf();
        mockMvc.perform(post(LOGIN_URL)
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(PROFILE_URL).session(csrf.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.displayName").value(displayName));
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
            // MockMvc does not model the servlet container's changeSessionId behavior; reuse the production strategy components.
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
