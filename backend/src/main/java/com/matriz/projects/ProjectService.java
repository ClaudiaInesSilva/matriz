package com.matriz.projects;

import com.matriz.security.MatrizUserPrincipal;
import com.matriz.tasks.TaskRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(String search) {
        String trimmedSearch = search == null ? null : search.trim();
        List<Project> projects = trimmedSearch == null || trimmedSearch.isEmpty()
                ? projectRepository.findAll()
                : projectRepository.findByNameContainingIgnoreCase(trimmedSearch);
        return projects.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId) {
        return toResponse(findProject(projectId));
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, MatrizUserPrincipal caller) {
        Project project = new Project(UUID.randomUUID(), request.name(), request.description(), caller.getUserId());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UpdateProjectRequest request, MatrizUserPrincipal caller) {
        Project project = findProject(projectId);
        requireOwner(project, caller);
        project.updateDetails(request.name(), request.description());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(UUID projectId, MatrizUserPrincipal caller) {
        Project project = findProject(projectId);
        requireOwner(project, caller);
        if (taskRepository.existsByProjectId(projectId)) {
            throw new ProjectHasTasksException();
        }
        projectRepository.delete(project);
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private void requireOwner(Project project, MatrizUserPrincipal caller) {
        if (!project.getOwnerId().equals(caller.getUserId())) {
            throw new ProjectAccessDeniedException();
        }
    }

    private ProjectResponse toResponse(Project project) {
        User owner = userRepository.findById(project.getOwnerId())
                .orElseThrow(() -> new IllegalStateException("Project owner account is missing"));
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                new UserSummaryResponse(owner.getId(), owner.getDisplayName()));
    }
}
