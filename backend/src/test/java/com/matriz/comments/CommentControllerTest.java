package com.matriz.comments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.security.MatrizUserPrincipal;
import com.matriz.security.SecurityConfiguration;
import com.matriz.tasks.TaskRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommentController.class)
@Import({
        SecurityConfiguration.class,
        CommentService.class,
        CommentExceptionHandler.class,
        CommentControllerTest.CommentTestConfiguration.class
})
class CommentControllerTest {
    private static final UUID TASK_ID = UUID.fromString("90000000-0000-4000-8000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("90000000-0000-4000-8000-000000000002");
    private static final UUID SPOOFED_AUTHOR_ID = UUID.fromString("90000000-0000-4000-8000-000000000003");
    private static final OffsetDateTime SPOOFED_TIME = OffsetDateTime.parse("2001-01-01T00:00:00Z");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    UserRepository userRepository;

    private List<Comment> savedComments;

    @BeforeEach
    void resetRepositories() {
        reset(taskRepository, commentRepository, userRepository);
        savedComments = new ArrayList<>();
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            savedComments.add(comment);
            return comment;
        });
        when(commentRepository.findAllByTaskId(TASK_ID)).thenAnswer(invocation -> List.copyOf(savedComments));
    }

    @Test
    void bothCommentRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentBody("A comment")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsCommentsForExistingTaskWithAuthorAndTimestamp() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        Comment comment = comment("Existing comment", AUTHOR_ID, OffsetDateTime.parse("2026-01-02T03:04:05Z"));
        when(commentRepository.findAllByTaskId(TASK_ID)).thenReturn(List.of(comment));
        when(userRepository.findAllById(List.of(AUTHOR_ID))).thenReturn(List.of(user(AUTHOR_ID, "Comment Author")));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID).with(authenticated(SPOOFED_AUTHOR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(comment.getId().toString()))
                .andExpect(jsonPath("$[0].taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$[0].author.userId").value(AUTHOR_ID.toString()))
                .andExpect(jsonPath("$[0].author.displayName").value("Comment Author"))
                .andExpect(jsonPath("$[0].author.username").doesNotExist())
                .andExpect(jsonPath("$[0].content").value("Existing comment"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-02T03:04:05Z"));
    }

    @Test
    void listingCommentsForMissingTaskReturnsNotFound() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(false);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID).with(authenticated(AUTHOR_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(commentRepository, userRepository);
    }

    @Test
    void createsCommentWithServerDerivedAuthorAndTimestampAndCanListItAgain() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, "Authenticated Author")));
        OffsetDateTime beforeRequest = OffsetDateTime.now();

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .with(csrf())
                        .with(authenticated(AUTHOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentBodyWithUntrustedFields("New comment")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").isNotEmpty())
                .andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.author.userId").value(AUTHOR_ID.toString()))
                .andExpect(jsonPath("$.author.displayName").value("Authenticated Author"))
                .andExpect(jsonPath("$.author.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content").value("New comment"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        verify(commentRepository).save(any(Comment.class));
        Comment saved = savedComments.getFirst();
        assertThat(saved.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(saved.getAuthorId()).isNotEqualTo(SPOOFED_AUTHOR_ID);
        assertThat(saved.getCreatedAt()).isAfter(beforeRequest);
        assertThat(saved.getCreatedAt()).isNotEqualTo(SPOOFED_TIME);
        assertThat(saved.getTaskId()).isEqualTo(TASK_ID);

        when(userRepository.findAllById(List.of(AUTHOR_ID)))
                .thenReturn(List.of(user(AUTHOR_ID, "Authenticated Author")));
        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID).with(authenticated(SPOOFED_AUTHOR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].commentId").value(saved.getId().toString()))
                .andExpect(jsonPath("$[0].author.userId").value(AUTHOR_ID.toString()))
                .andExpect(jsonPath("$[0].content").value("New comment"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    @Test
    void creationRequiresAnExistingTask() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(false);

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .with(csrf())
                        .with(authenticated(AUTHOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentBody("Valid comment")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(commentRepository, userRepository);
    }

    @Test
    void acceptsOneAndTwoThousandCharacterComments() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, "Authenticated Author")));

        for (String content : List.of("x", "x".repeat(2000))) {
            mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                            .with(csrf())
                            .with(authenticated(AUTHOR_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(commentBody(content)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value(content));
        }
    }

    @Test
    void rejectsEmptyWhitespaceOnlyAndOverLimitCommentContent() throws Exception {
        for (String content : List.of("", "   ", "\t\n", "x".repeat(2001))) {
            mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                            .with(csrf())
                            .with(authenticated(AUTHOR_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(commentBody(content)))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .with(csrf())
                        .with(authenticated(AUTHOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskRepository, commentRepository, userRepository);
    }

    private static Comment comment(String content, UUID authorId, OffsetDateTime createdAt) {
        return new Comment(UUID.randomUUID(), TASK_ID, authorId, content, createdAt);
    }

    private static User user(UUID id, String displayName) {
        return new User(id, "private.username", "private.password.hash", displayName);
    }

    private static String commentBody(String content) {
        String escapedContent = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"content\":\"" + escapedContent + "\"}";
    }

    private static String commentBodyWithUntrustedFields(String content) {
        return """
                {"content":"%s","authorId":"%s","createdAt":"%s"}
                """.formatted(content, SPOOFED_AUTHOR_ID, SPOOFED_TIME);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(UUID userId) {
        MatrizUserPrincipal principal =
                new MatrizUserPrincipal(userId, "user." + userId, "!DISABLED!", "Test User");
        return authentication(new UsernamePasswordAuthenticationToken(principal, "", AuthorityUtils.NO_AUTHORITIES));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CommentTestConfiguration {
        @Bean
        TaskRepository taskRepository() {
            return mock(TaskRepository.class);
        }

        @Bean
        CommentRepository commentRepository() {
            return mock(CommentRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("No test account");
            };
        }
    }
}
