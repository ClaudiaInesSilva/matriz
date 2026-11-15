package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.projects.Project;
import com.matriz.projects.ProjectExceptionHandler;
import com.matriz.projects.ProjectRepository;
import com.matriz.security.MatrizUserPrincipal;
import com.matriz.security.SecurityConfiguration;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TaskController.class)
@Import({
        SecurityConfiguration.class,
        TaskService.class,
        TaskExceptionHandler.class,
        ProjectExceptionHandler.class,
        TaskControllerTest.TaskTestConfiguration.class
})
class TaskControllerTest {
    private static final UUID PROJECT_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("70000000-0000-4000-8000-000000000002");
    private static final UUID OWNER_ID = UUID.fromString("70000000-0000-4000-8000-000000000003");
    private static final UUID CREATOR_ID = UUID.fromString("70000000-0000-4000-8000-000000000004");
    private static final UUID OTHER_ID = UUID.fromString("70000000-0000-4000-8000-000000000005");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    ProjectRepository projectRepository;

    @BeforeEach
    void resetRepositories() {
        reset(taskRepository, projectRepository);
    }

    @Test
    void taskRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid title\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks/{taskId}", TASK_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsTaskUsingAuthenticatedCreatorAndDefaultsStatusAndPriority() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(CREATOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  New task  \",\"description\":\"Details\",\"creatorId\":\"" + OTHER_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/tasks/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.title").value("New task"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        var saved = org.mockito.ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(saved.capture());
        assertThat(saved.getValue().getCreatorId()).isEqualTo(CREATOR_ID);
        assertThat(saved.getValue().getCreatorId()).isNotEqualTo(OTHER_ID);
    }

    @Test
    void rejectsShortBlankAndWhitespaceOnlyTitles() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        for (String title : List.of("ab", "x".repeat(151), "   ", "\t\n")) {
            mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                            .with(csrf())
                            .with(authenticated(CREATOR_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"" + title.replace("\t", "\\t").replace("\n", "\\n") + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(taskRepository);
    }

    @Test
    void rejectsDescriptionLongerThanTwoThousandCharacters() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        String content = "{\"title\":\"Valid title\",\"description\":\"" + "x".repeat(2001) + "\"}";

        mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(CREATOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(taskRepository);
    }

    @Test
    void createAndListReturnNotFoundForMissingProject() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(CREATOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID).with(authenticated(CREATOR_ID)))
                .andExpect(status().isNotFound());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void listsProjectTasksAndAppliesEitherOrBothFilters() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        PageRequest defaultPage = PageRequest.of(0, 20);
        when(taskRepository.findByProjectId(PROJECT_ID, defaultPage))
                .thenReturn(new PageImpl<>(List.of(task()), defaultPage, 1));
        when(taskRepository.findByProjectIdAndStatus(PROJECT_ID, TaskStatus.DONE, defaultPage))
                .thenReturn(new PageImpl<>(List.of(task()), defaultPage, 1));
        when(taskRepository.findByProjectIdAndPriority(PROJECT_ID, TaskPriority.HIGH, defaultPage))
                .thenReturn(new PageImpl<>(List.of(task()), defaultPage, 1));
        when(taskRepository.findByProjectIdAndStatusAndPriority(
                PROJECT_ID, TaskStatus.DONE, TaskPriority.HIGH, defaultPage))
                .thenReturn(new PageImpl<>(List.of(task()), defaultPage, 1));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID).with(authenticated(OTHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("status", "DONE").with(authenticated(OTHER_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("priority", "HIGH").with(authenticated(OTHER_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("status", "DONE").queryParam("priority", "HIGH")
                        .with(authenticated(OTHER_ID)))
                .andExpect(status().isOk());

        verify(taskRepository).findByProjectId(PROJECT_ID, defaultPage);
        verify(taskRepository).findByProjectIdAndStatus(PROJECT_ID, TaskStatus.DONE, defaultPage);
        verify(taskRepository).findByProjectIdAndPriority(PROJECT_ID, TaskPriority.HIGH, defaultPage);
        verify(taskRepository).findByProjectIdAndStatusAndPriority(
                PROJECT_ID, TaskStatus.DONE, TaskPriority.HIGH, defaultPage);
    }

    @Test
    void appliesExplicitPaginationTogetherWithBothFilters() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        PageRequest requestedPage = PageRequest.of(2, 1);
        when(taskRepository.findByProjectIdAndStatusAndPriority(
                PROJECT_ID, TaskStatus.DONE, TaskPriority.HIGH, requestedPage))
                .thenReturn(new PageImpl<>(List.of(task()), requestedPage, 3));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("status", "DONE")
                        .queryParam("priority", "HIGH")
                        .queryParam("page", "2")
                        .queryParam("size", "1")
                        .with(authenticated(OTHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(1))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(taskRepository).findByProjectIdAndStatusAndPriority(
                PROJECT_ID, TaskStatus.DONE, TaskPriority.HIGH, requestedPage);
    }

    @Test
    void rejectsPageAndSizeOutsidePaginationBounds() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("page", "-1").with(authenticated(OTHER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("size", "0").with(authenticated(OTHER_ID)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("size", "101").with(authenticated(OTHER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidStatusOrPriorityFilters() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("status", "NOT_A_STATUS").with(authenticated(OTHER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(get("/api/projects/{projectId}/tasks", PROJECT_ID)
                        .queryParam("priority", "NOT_A_PRIORITY").with(authenticated(OTHER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrievesIndividualTaskAndReturnsNotFoundWhenMissing() throws Exception {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));

        mockMvc.perform(get("/api/tasks/{taskId}", TASK_ID).with(authenticated(OTHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.title").value("Existing task"));

        UUID missingId = UUID.fromString("70000000-0000-4000-8000-000000000099");
        mockMvc.perform(get("/api/tasks/{taskId}", missingId).with(authenticated(OTHER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void updatesTaskFieldsAndStatusAndPriorityWithoutChangingCreatorOrProject() throws Exception {
        Task existing = task();
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Updated task  \",\"description\":\"Updated\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"creatorId\":\"" + OTHER_ID + "\",\"projectId\":\"" + OTHER_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated task"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        assertThat(existing.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(existing.getCreatorId()).isEqualTo(CREATOR_ID);
        assertThat(existing.getDescription()).isEqualTo("Updated");
    }

    @Test
    void rejectsUpdateWithInvalidFieldsOrMissingTask() throws Exception {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        mockMvc.perform(put("/api/tasks/{taskId}", TASK_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"status\":\"TODO\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest());

        UUID missingId = UUID.fromString("70000000-0000-4000-8000-000000000099");
        when(taskRepository.findById(missingId)).thenReturn(Optional.empty());
        mockMvc.perform(put("/api/tasks/{taskId}", missingId)
                        .with(csrf())
                        .with(authenticated(OTHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatorCanDeleteTask() throws Exception {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID).with(csrf()).with(authenticated(CREATOR_ID)))
                .andExpect(status().isNoContent());

        verify(taskRepository).delete(any(Task.class));
    }

    @Test
    void projectOwnerCanDeleteTaskCreatedByAnotherUser() throws Exception {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID).with(csrf()).with(authenticated(OWNER_ID)))
                .andExpect(status().isNoContent());

        verify(taskRepository).delete(any(Task.class));
    }

    @Test
    void anotherAuthenticatedUserCannotDeleteTask() throws Exception {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID).with(csrf()).with(authenticated(OTHER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());

        org.mockito.Mockito.verify(taskRepository, org.mockito.Mockito.never()).delete(any(Task.class));
    }

    @Test
    void deletingMissingTaskReturnsNotFound() throws Exception {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/tasks/{taskId}", TASK_ID).with(csrf()).with(authenticated(OWNER_ID)))
                .andExpect(status().isNotFound());
    }

    private static String updateBody() {
        return "{\"title\":\"Valid title\",\"status\":\"TODO\",\"priority\":\"MEDIUM\"}";
    }

    private static Task task() {
        return new Task(TASK_ID, PROJECT_ID, CREATOR_ID, "Existing task", "Details", TaskStatus.DONE, TaskPriority.HIGH);
    }

    private static Project project() {
        return new Project(PROJECT_ID, "Project name", null, OWNER_ID);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(UUID userId) {
        MatrizUserPrincipal principal = new MatrizUserPrincipal(userId, "user." + userId, "!DISABLED!", "Test User");
        return authentication(new UsernamePasswordAuthenticationToken(principal, "", AuthorityUtils.NO_AUTHORITIES));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TaskTestConfiguration {
        @Bean
        TaskRepository taskRepository() {
            return mock(TaskRepository.class);
        }

        @Bean
        ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("No test account");
            };
        }
    }
}
