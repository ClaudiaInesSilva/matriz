package com.matriz.assignments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.projects.ProjectRepository;
import com.matriz.security.MatrizUserPrincipal;
import com.matriz.security.SecurityConfiguration;
import com.matriz.tasks.TaskRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AssignmentController.class)
@Import({
        SecurityConfiguration.class,
        AssignmentService.class,
        AssignmentExceptionHandler.class,
        AssignmentControllerTest.AssignmentTestConfiguration.class
})
class AssignmentControllerTest {
    private static final UUID PROJECT_ID = UUID.fromString("80000000-0000-4000-8000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("80000000-0000-4000-8000-000000000002");
    private static final UUID FIRST_USER_ID = UUID.fromString("80000000-0000-4000-8000-000000000003");
    private static final UUID SECOND_USER_ID = UUID.fromString("80000000-0000-4000-8000-000000000004");
    private static final UUID OTHER_USER_ID = UUID.fromString("80000000-0000-4000-8000-000000000005");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskAssignmentRepository assignmentRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void resetRepositories() {
        reset(projectRepository, taskRepository, assignmentRepository, userRepository);
    }

    @Test
    void allAssignmentRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/eligible-assignees", PROJECT_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks/{taskId}/assignees", TASK_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(FIRST_USER_ID)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/tasks/{taskId}/assignees/{userId}", TASK_ID, FIRST_USER_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsAllEligibleUsersUsingOnlyUserSummaryFields() throws Exception {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of(user(FIRST_USER_ID, "First User")));

        mockMvc.perform(get("/api/projects/{projectId}/eligible-assignees", PROJECT_ID)
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(FIRST_USER_ID.toString()))
                .andExpect(jsonPath("$[0].displayName").value("First User"))
                .andExpect(jsonPath("$[0].username").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void eligibleUsersRequestRejectsMissingProject() throws Exception {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

        mockMvc.perform(get("/api/projects/{projectId}/eligible-assignees", PROJECT_ID)
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(userRepository);
    }

    @Test
    void listsMultipleTaskAssigneesUsingOnlyUserSummaryFields() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(assignmentRepository.findAllById_TaskId(TASK_ID)).thenReturn(List.of(
                new TaskAssignment(new TaskAssignmentId(TASK_ID, FIRST_USER_ID)),
                new TaskAssignment(new TaskAssignmentId(TASK_ID, SECOND_USER_ID))));
        when(userRepository.findAllById(List.of(FIRST_USER_ID, SECOND_USER_ID)))
                .thenReturn(List.of(user(FIRST_USER_ID, "First User"), user(SECOND_USER_ID, "Second User")));

        mockMvc.perform(get("/api/tasks/{taskId}/assignees", TASK_ID).with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(FIRST_USER_ID.toString()))
                .andExpect(jsonPath("$[0].displayName").value("First User"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[1].userId").value(SECOND_USER_ID.toString()))
                .andExpect(jsonPath("$[1].displayName").value("Second User"));
    }

    @Test
    void listingAssigneesRejectsMissingTask() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(false);

        mockMvc.perform(get("/api/tasks/{taskId}/assignees", TASK_ID).with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(assignmentRepository, userRepository);
    }

    @Test
    void anyAuthenticatedUserCanAssignAnExistingUser() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(userRepository.existsById(FIRST_USER_ID)).thenReturn(true);
        when(assignmentRepository.existsById(new TaskAssignmentId(TASK_ID, FIRST_USER_ID))).thenReturn(false);

        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(FIRST_USER_ID)))
                .andExpect(status().isCreated());

        var saved = org.mockito.ArgumentCaptor.forClass(TaskAssignment.class);
        verify(assignmentRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getId().getTaskId()).isEqualTo(TASK_ID);
        assertThat(saved.getValue().getId().getUserId()).isEqualTo(FIRST_USER_ID);
    }

    @Test
    void duplicateAssignmentReturnsConflict() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(userRepository.existsById(FIRST_USER_ID)).thenReturn(true);
        when(assignmentRepository.existsById(new TaskAssignmentId(TASK_ID, FIRST_USER_ID))).thenReturn(true);

        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(FIRST_USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void duplicateAssignmentRejectedByDatabaseReturnsConflict() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(userRepository.existsById(FIRST_USER_ID)).thenReturn(true);
        when(assignmentRepository.existsById(new TaskAssignmentId(TASK_ID, FIRST_USER_ID))).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate task/user pair"))
                .when(assignmentRepository)
                .saveAndFlush(any(TaskAssignment.class));

        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(FIRST_USER_ID)))
                .andExpect(status().isConflict());
    }

    @Test
    void assignmentRejectsMissingTaskAndMissingUser() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(false);
        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(FIRST_USER_ID)))
                .andExpect(status().isNotFound());

        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(userRepository.existsById(FIRST_USER_ID)).thenReturn(false);
        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(FIRST_USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignmentRequiresAUserId() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/assignees", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskRepository, assignmentRepository, userRepository);
    }

    @Test
    void removalReturnsNoContentForExistingAssignment() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(assignmentRepository.existsById(new TaskAssignmentId(TASK_ID, FIRST_USER_ID))).thenReturn(true);

        mockMvc.perform(delete("/api/tasks/{taskId}/assignees/{userId}", TASK_ID, FIRST_USER_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isNoContent());

        verify(assignmentRepository).deleteById(new TaskAssignmentId(TASK_ID, FIRST_USER_ID));
    }

    @Test
    void removalRejectsMissingTaskOrAssignment() throws Exception {
        when(taskRepository.existsById(TASK_ID)).thenReturn(false);
        mockMvc.perform(delete("/api/tasks/{taskId}/assignees/{userId}", TASK_ID, FIRST_USER_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isNotFound());

        when(taskRepository.existsById(TASK_ID)).thenReturn(true);
        when(assignmentRepository.existsById(new TaskAssignmentId(TASK_ID, FIRST_USER_ID))).thenReturn(false);
        mockMvc.perform(delete("/api/tasks/{taskId}/assignees/{userId}", TASK_ID, FIRST_USER_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isNotFound());
    }

    private static User user(UUID id, String displayName) {
        return new User(id, "private.username", "private.password.hash", displayName);
    }

    private static String assignBody(UUID userId) {
        return "{\"userId\":\"" + userId + "\"}";
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(UUID userId) {
        MatrizUserPrincipal principal =
                new MatrizUserPrincipal(userId, "user." + userId, "!DISABLED!", "Test User");
        return authentication(new UsernamePasswordAuthenticationToken(principal, "", AuthorityUtils.NO_AUTHORITIES));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AssignmentTestConfiguration {
        @Bean
        ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        TaskRepository taskRepository() {
            return mock(TaskRepository.class);
        }

        @Bean
        TaskAssignmentRepository assignmentRepository() {
            return mock(TaskAssignmentRepository.class);
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
