package com.matriz.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import static org.hamcrest.Matchers.matchesPattern;

import com.matriz.security.MatrizUserPrincipal;
import com.matriz.security.SecurityConfiguration;
import com.matriz.tasks.TaskRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;
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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

@WebMvcTest(controllers = ProjectController.class)
@Import({
        SecurityConfiguration.class,
        ProjectService.class,
        ProjectExceptionHandler.class,
        ProjectControllerTest.ProjectTestConfiguration.class
})
class ProjectControllerTest {
    private static final UUID PROJECT_ID = UUID.fromString("60000000-0000-4000-8000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("60000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_USER_ID = UUID.fromString("60000000-0000-4000-8000-000000000003");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void resetRepositories() {
        reset(projectRepository, taskRepository, userRepository);
    }

    @Test
    void projectRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid project\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsProjectWithAuthenticatedPrincipalAsOwnerIgnoringClientOwnerId() throws Exception {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(user(OTHER_USER_ID, "Other User")));

        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alpha project","description":"Description","ownerId":"%s"}
                                """.formatted(OWNER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/projects/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.owner.userId").value(OTHER_USER_ID.toString()))
                .andExpect(jsonPath("$.owner.displayName").value("Other User"));

        var saved = org.mockito.ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(OTHER_USER_ID);
        assertThat(saved.getValue().getOwnerId()).isNotEqualTo(OWNER_ID);
    }

    @Test
    void rejectsProjectNameOutsideDocumentedLength() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .with(authenticated(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(projectRepository);
    }

    @Test
    void listsAllProjectsAndAppliesTrimmedCaseInsensitivePartialSearch() throws Exception {
        Project owned = project(PROJECT_ID, "Alpha plan", OWNER_ID);
        Project other = project(UUID.fromString("60000000-0000-4000-8000-000000000004"), "Beta plan", OTHER_USER_ID);
        when(projectRepository.findAll()).thenReturn(List.of(owned, other));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID, "Owner")));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(user(OTHER_USER_ID, "Other")));

        mockMvc.perform(get("/api/projects").with(authenticated(OWNER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alpha plan"))
                .andExpect(jsonPath("$[1].name").value("Beta plan"));
        verify(projectRepository).findAll();

        when(projectRepository.findByNameContainingIgnoreCase("Alpha")).thenReturn(List.of(owned));
        mockMvc.perform(get("/api/projects")
                        .queryParam("search", "  Alpha  ")
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alpha plan"));
        verify(projectRepository).findByNameContainingIgnoreCase("Alpha");
    }

    @Test
    void anyAuthenticatedUserCanViewAProject() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID, "Owned project", OWNER_ID)));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID, "Owner")));

        mockMvc.perform(get("/api/projects/{projectId}", PROJECT_ID).with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.owner.userId").value(OWNER_ID.toString()));
    }

    @Test
    void ownerCanUpdateProjectWithoutChangingOwnership() throws Exception {
        Project project = project(PROJECT_ID, "Old name", OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID, "Owner")));

        mockMvc.perform(put("/api/projects/{projectId}", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated project\",\"description\":\"New description\",\"ownerId\":\"%s\"}".formatted(OTHER_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated project"))
                .andExpect(jsonPath("$.owner.userId").value(OWNER_ID.toString()));

        assertThat(project.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(project.getDescription()).isEqualTo("New description");
    }

    @Test
    void nonOwnerCannotUpdateOrDeleteProject() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID, "Owned project", OWNER_ID)));

        mockMvc.perform(put("/api/projects/{projectId}", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Attempted change\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/projects/{projectId}", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(OTHER_USER_ID)))
                .andExpect(status().isForbidden());

        verify(projectRepository, org.mockito.Mockito.times(2)).findById(PROJECT_ID);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void ownerCanDeleteProjectWithoutTasks() throws Exception {
        Project project = project(PROJECT_ID, "Empty project", OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(taskRepository.existsByProjectId(PROJECT_ID)).thenReturn(false);

        mockMvc.perform(delete("/api/projects/{projectId}", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(OWNER_ID)))
                .andExpect(status().isNoContent());

        verify(projectRepository).delete(project);
    }

    @Test
    void ownerCannotDeleteProjectWhileTasksRemain() throws Exception {
        Project project = project(PROJECT_ID, "Project with tasks", OWNER_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(taskRepository.existsByProjectId(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/projects/{projectId}", PROJECT_ID)
                        .with(csrf())
                        .with(authenticated(OWNER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(projectRepository, org.mockito.Mockito.never()).delete(any(Project.class));
    }

    @Test
    void missingProjectReturnsNotFound() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/{projectId}", PROJECT_ID).with(authenticated(OWNER_ID)))
                .andExpect(status().isNotFound());
    }

    private static Project project(UUID id, String name, UUID ownerId) {
        return new Project(id, name, null, ownerId);
    }

    private static User user(UUID id, String displayName) {
        return new User(id, displayName.toLowerCase().replace(' ', '.'), "!DISABLED!", displayName);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(UUID userId) {
        MatrizUserPrincipal principal = new MatrizUserPrincipal(userId, "user." + userId, "!DISABLED!", "Test User");
        return authentication(new UsernamePasswordAuthenticationToken(principal, "", AuthorityUtils.NO_AUTHORITIES));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProjectTestConfiguration {
        @Bean
        ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        TaskRepository taskRepository() {
            return mock(TaskRepository.class);
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
