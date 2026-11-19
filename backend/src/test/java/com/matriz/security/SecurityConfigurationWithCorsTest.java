package com.matriz.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import({SecurityConfiguration.class, SecurityConfigurationWithCorsTest.UserDetailsTestConfiguration.class})
@org.springframework.test.context.TestPropertySource(properties = "matriz.cors.allowed-origins=https://cors-test.example.com")
class SecurityConfigurationWithCorsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void crossOriginPreflightAllowedWhenCorsOriginConfigured() throws Exception {
        mockMvc.perform(options("/api/probe/private")
                        .header("Origin", "https://cors-test.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-CSRF-TOKEN,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://cors-test.example.com"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-CSRF-TOKEN")))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-CSRF-TOKEN")));
    }

    @Test
    void crossOriginRequestFromDisallowedOriginRejected() throws Exception {
        mockMvc.perform(get("/api/probe/private")
                        .header("Origin", "https://evil.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertNull(allowOrigin,
                            "CORS headers should not be sent for disallowed origins");
                });
    }

    @Test
    void crossOriginGetRequestFromAllowedOriginPermitted() throws Exception {
        mockMvc.perform(get("/api/probe/private")
                        .header("Origin", "https://cors-test.example.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://cors-test.example.com"));
    }

    static class UserDetailsTestConfiguration {
        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("No test account");
            };
        }
    }
}
