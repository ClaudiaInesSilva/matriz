package com.matriz.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test that starts the actual embedded Tomcat server with the render profile
 * and inspects the Set-Cookie header to verify SameSite=None and Secure attributes.
 *
 * This test validates the complete chain:
 * property → ServerProperties → TomcatServletWebServerFactoryCustomizer → Rfc6265CookieProcessor → Set-Cookie header.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("render")
class RenderSameSiteCookieIntegrationTest {

    @DynamicPropertySource
    static void configureH2Database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:matriz_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @LocalServerPort
    private int port;

    private static HttpClient httpClient;

    @BeforeAll
    static void setupHttpClient() {
        httpClient = HttpClient.newHttpClient();
    }

    @Test
    @DisplayName("JSESSIONID cookie should have SameSite=None and Secure in render profile")
    void sessionCookieHasSameSiteNoneAndSecure() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.port + "/api/auth/csrf"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");

        // Log all Set-Cookie headers for debugging
        System.out.println("Set-Cookie headers: " + setCookieHeaders);

        assertThat(setCookieHeaders)
                .as("At least one Set-Cookie header should be present")
                .isNotEmpty();

        // Find the JSESSIONID cookie
        Optional<String> jsessionidCookie = setCookieHeaders.stream()
                .filter(h -> h.toUpperCase().startsWith("JSESSIONID="))
                .findFirst();

        assertThat(jsessionidCookie)
                .as("A JSESSIONID cookie should be present in the response")
                .isPresent();

        String cookie = jsessionidCookie.get();
        String cookieUpper = cookie.toUpperCase();

        assertThat(cookieUpper)
                .as("JSESSIONID cookie should contain SameSite=None (case-insensitive): %s", cookie)
                .contains("SAMESITE=NONE");

        assertThat(cookieUpper)
                .as("JSESSIONID cookie should contain Secure (case-insensitive): %s", cookie)
                .contains("SECURE");
    }

    @Test
    @DisplayName("Print effective Set-Cookie headers for debugging")
    void printEffectiveSetCookieHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + this.port + "/api/auth/csrf"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");

        System.out.println("=== Set-Cookie headers from render profile ===");
        if (!setCookieHeaders.isEmpty()) {
            setCookieHeaders.forEach(h -> System.out.println("  " + h));
        } else {
            System.out.println("  (no Set-Cookie headers)");
        }
        System.out.println("===========================================");
    }
}
