package com.matriz.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("render")
@EnabledIfEnvironmentVariable(named = "MATRIZ_TEST_DB_PASSWORD", matches = ".+")
class PostgresDemoLoginIntegrationTest {
    private static final String TEST_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/matriz_test";
    private static final String TEST_PASSWORD_ENV = "MATRIZ_TEST_DB_PASSWORD";
    private static final String TEST_USERNAME_ENV = "MATRIZ_TEST_DB_USERNAME";

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void useDedicatedTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_DATABASE_URL);
        registry.add("spring.datasource.username", PostgresDemoLoginIntegrationTest::requiredTestUsername);
        registry.add("spring.datasource.password", () -> System.getenv(TEST_PASSWORD_ENV));
        registry.add("matriz.demo-accounts.auto-login-enabled", () -> "true");
    }

    @Test
    void demoLoginSucceedsForDemoAlex() throws Exception {
        MockHttpSession session = loginDemo("demo.alex");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo.alex"));
    }

    @Test
    void demoLoginSucceedsForDemoSam() throws Exception {
        MockHttpSession session = loginDemo("demo.sam");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo.sam"));
    }

    @Test
    void ordinaryLoginRejectsDemoAlexWithDisabledHash() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.token");
        String csrfHeader = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.headerName");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .header(csrfHeader, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo.alex\",\"password\":\"any-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession loginDemo(String username) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.token");
        String csrfHeader = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.headerName");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/demo-login")
                        .session(session)
                        .header(csrfHeader, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private static String requiredTestUsername() {
        String username = System.getenv(TEST_USERNAME_ENV);
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(TEST_USERNAME_ENV + " must be set to run PostgreSQL integration tests");
        }
        return username;
    }
}
