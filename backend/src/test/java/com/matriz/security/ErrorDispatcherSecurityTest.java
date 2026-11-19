package com.matriz.security;

import com.matriz.users.User;
import com.matriz.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:matriz_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "matriz.demo-accounts.authentication-enabled=false",
        "matriz.demo-accounts.auto-login-enabled=false"
})
class ErrorDispatcherSecurityTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    RestTestClient client;
    String sessionCookie;
    String csrfToken;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        // Create a test user in the H2 database
        User user = new User(
                UUID.fromString("50000000-0000-4000-8000-000000000001"),
                "test.user",
                passwordEncoder.encode("test-password"),
                "Test User"
        );
        userRepository.save(user);
    }

    @Test
    void errorDispatcherDoesNotMaskWith401() {
        // Step 1: Get CSRF token and session cookie
        getCsrftoken();

        // Step 2: Login to authenticate the session
        login();

        // Step 3: Access the throwing endpoint with the authenticated session.
        // The controller throws a RuntimeException, triggering a servlet ERROR dispatch.
        // With DispatcherType.ERROR permitted, the response should be 500 (Internal Server Error),
        // not 401 (which would mean the error was masked by the authentication entry point).
        RestTestClient.ResponseSpec spec = client.get()
                .uri("/api/probe/throw-error")
                .header("Cookie", sessionCookie)
                .header("X-CSRF-TOKEN", csrfToken)
                .exchange();
        RestTestClientResponse response = RestTestClientResponse.from(spec);

        assertThat(response)
                .as("ERROR dispatch should return 500 Internal Server Error")
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointReturns401() {
        // Normal unauthenticated requests to protected endpoints must still return 401.
        // This confirms the ERROR dispatcher change does not weaken normal authorization.
        RestTestClient.ResponseSpec spec = client.get()
                .uri("/api/probe/private")
                .exchange();
        RestTestClientResponse response = RestTestClientResponse.from(spec);

        assertThat(response)
                .as("Unauthenticated request to protected endpoint must return 401")
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void getCsrftoken() {
        RestTestClient.ResponseSpec spec = client.get()
                .uri("/api/auth/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionCookie)
                .exchange();

        // Extract session cookie from Set-Cookie header
        String setCookie = spec.returnResult().getResponseHeaders().getFirst("Set-Cookie");
        if (setCookie != null) {
            // Parse "JSESSIONID=xxx; Path=/; HttpOnly" -> "JSESSIONID=xxx"
            sessionCookie = setCookie.split(";")[0];
        }

        // Extract CSRF token from JSON body
        String body = spec.returnResult(String.class).getResponseBody();
        int tokenStart = body.indexOf("\"token\":\"") + 9;
        int tokenEnd = body.indexOf("\"", tokenStart);
        csrfToken = body.substring(tokenStart, tokenEnd);
    }

    private void login() {
        RestTestClient.ResponseSpec spec = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cookie", sessionCookie)
                .header("X-CSRF-TOKEN", csrfToken)
                .body("{\"username\":\"test.user\",\"password\":\"test-password\"}")
                .exchange();
        RestTestClientResponse response = RestTestClientResponse.from(spec);

        assertThat(response)
                .as("Login should succeed")
                .hasStatus(HttpStatus.OK);

        // Update session cookie after login (session ID may have rotated)
        String setCookie = spec.returnResult().getResponseHeaders().getFirst("Set-Cookie");
        if (setCookie != null) {
            sessionCookie = setCookie.split(";")[0];
        }

        // Get the new CSRF token after login
        getCsrftoken();
    }
}
