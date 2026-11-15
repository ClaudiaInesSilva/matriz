package com.matriz.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.matriz.projects.Project;
import com.matriz.projects.ProjectRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MATRIZ_TEST_DB_PASSWORD", matches = ".+")
class PostgresAssignmentCommentPersistenceTest {
    private static final String TEST_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/matriz_test";
    private static final String TEST_PASSWORD_ENV = "MATRIZ_TEST_DB_PASSWORD";
    private static final String TEST_USERNAME_ENV = "MATRIZ_TEST_DB_USERNAME";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void useDedicatedTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_DATABASE_URL);
        registry.add("spring.datasource.username", PostgresAssignmentCommentPersistenceTest::requiredTestUsername);
        registry.add("spring.datasource.password", () -> System.getenv(TEST_PASSWORD_ENV));
    }

    @BeforeEach
    void verifyDatabaseTarget() {
        assertThat(jdbcTemplate.queryForObject("SELECT current_database()", String.class))
                .isEqualTo("matriz_test");
    }

    @Test
    void persistsAssignmentsAcrossSeparateHttpRequests() throws Exception {
        TestUser owner = createUser("Persistence Owner");
        User secondAssignee = persistUser("Persistence Assignee");
        Project project = projectRepository.saveAndFlush(new Project(
                UUID.randomUUID(),
                "Assignment persistence " + UUID.randomUUID(),
                null,
                owner.id()));
        MockHttpSession session = login(owner);

        MvcResult createdTask = mockMvc.perform(post("/api/projects/{projectId}/tasks", project.getId())
                        .session(session)
                        .header(csrfHeader(session), csrfToken(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Persistence task\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID taskId = UUID.fromString(JsonPath.read(createdTask.getResponse().getContentAsString(), "$.taskId"));

        assignUser(taskId, owner.id(), session);
        assertAssigneeIds(taskId, session, List.of(owner.id()));

        assignUser(taskId, secondAssignee.getId(), session);
        assertAssigneeIds(taskId, session, List.of(owner.id(), secondAssignee.getId()));

        mockMvc.perform(delete("/api/tasks/{taskId}/assignees/{userId}", taskId, owner.id())
                        .session(session)
                        .header(csrfHeader(session), csrfToken(session)))
                .andExpect(status().isNoContent());
        assertAssigneeIds(taskId, session, List.of(secondAssignee.getId()));
    }

    @Test
    void persistsCommentAcrossSeparateHttpRequests() throws Exception {
        TestUser author = createUser("Comment Persistence Author");
        Project project = projectRepository.saveAndFlush(new Project(
                UUID.randomUUID(),
                "Comment persistence " + UUID.randomUUID(),
                null,
                author.id()));
        MockHttpSession session = login(author);

        MvcResult createdTask = mockMvc.perform(post("/api/projects/{projectId}/tasks", project.getId())
                        .session(session)
                        .header(csrfHeader(session), csrfToken(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Comment persistence task\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID taskId = UUID.fromString(JsonPath.read(createdTask.getResponse().getContentAsString(), "$.taskId"));
        String content = "Persisted through the real PostgreSQL repository";
        OffsetDateTime beforeCreate = OffsetDateTime.now();

        MvcResult createdComment = mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .session(session)
                        .header(csrfHeader(session), csrfToken(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String commentId = JsonPath.read(createdComment.getResponse().getContentAsString(), "$.commentId");
        String responseCreatedAt = JsonPath.read(createdComment.getResponse().getContentAsString(), "$.createdAt");
        assertThat(UUID.fromString(JsonPath.read(
                createdComment.getResponse().getContentAsString(), "$.author.userId")))
                .isEqualTo(author.id());
        OffsetDateTime creationTime = OffsetDateTime.parse(responseCreatedAt);
        assertThat(creationTime).isAfter(beforeCreate);

        MvcResult listedComments = mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String commentsJson = listedComments.getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(commentsJson, "$.length()")).isEqualTo(1);
        assertThat(JsonPath.<String>read(commentsJson, "$[0].commentId")).isEqualTo(commentId);
        assertThat(JsonPath.<String>read(commentsJson, "$[0].content")).isEqualTo(content);
        assertThat(UUID.fromString(JsonPath.read(commentsJson, "$[0].author.userId"))).isEqualTo(author.id());
        assertThat(OffsetDateTime.parse(JsonPath.<String>read(commentsJson, "$[0].createdAt")))
                .isEqualTo(creationTime.truncatedTo(ChronoUnit.MICROS));
    }

    private TestUser createUser(String displayName) {
        UUID id = UUID.randomUUID();
        String username = "persistence-" + UUID.randomUUID();
        String password = UUID.randomUUID().toString();
        userRepository.saveAndFlush(new User(id, username, passwordEncoder.encode(password), displayName));
        return new TestUser(id, username, password);
    }

    private User persistUser(String displayName) {
        UUID id = UUID.randomUUID();
        return userRepository.saveAndFlush(new User(
                id,
                "assignee-" + UUID.randomUUID(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                displayName));
    }

    private MockHttpSession login(TestUser user) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.token");
        String csrfHeader = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.headerName");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .header(csrfHeader, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user.username() + "\",\"password\":\"" + user.password() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private String csrfToken(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String csrfHeader(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.headerName");
    }

    private void assignUser(UUID taskId, UUID userId, MockHttpSession session) throws Exception {
        String csrfHeader = csrfHeader(session);
        String csrfToken = csrfToken(session);
        mockMvc.perform(post("/api/tasks/{taskId}/assignees", taskId)
                        .session(session)
                        .header(csrfHeader, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isCreated());
    }

    private void assertAssigneeIds(UUID taskId, MockHttpSession session, List<UUID> expectedIds) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tasks/{taskId}/assignees", taskId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<Integer>read(result.getResponse().getContentAsString(), "$.length()"))
                .isEqualTo(expectedIds.size());
        assertThat(JsonPath.<List<String>>read(result.getResponse().getContentAsString(), "$[*].userId"))
                .containsExactlyInAnyOrderElementsOf(expectedIds.stream().map(UUID::toString).toList());
    }

    private static String requiredTestUsername() {
        String username = System.getenv(TEST_USERNAME_ENV);
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(TEST_USERNAME_ENV + " must be set to run PostgreSQL integration tests");
        }
        return username;
    }

    private record TestUser(UUID id, String username, String password) {
    }
}
