package com.matriz.tasks;

import com.matriz.projects.Project;
import com.matriz.projects.ProjectNotFoundException;
import com.matriz.projects.ProjectRepository;
import com.matriz.security.MatrizUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public TaskResponse create(UUID projectId, CreateTaskRequest request, MatrizUserPrincipal caller) {
        requireProject(projectId);
        Task task = new Task(
                UUID.randomUUID(),
                projectId,
                caller.getUserId(),
                request.title(),
                request.description(),
                request.status() == null ? TaskStatus.TODO : request.status(),
                request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskPageResponse list(UUID projectId, TaskStatus status, TaskPriority priority, int page, int size) {
        requireProject(projectId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> tasks;
        if (status != null && priority != null) {
            tasks = taskRepository.findByProjectIdAndStatusAndPriority(projectId, status, priority, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByProjectIdAndStatus(projectId, status, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByProjectIdAndPriority(projectId, priority, pageable);
        } else {
            tasks = taskRepository.findByProjectId(projectId, pageable);
        }
        List<TaskResponse> taskResponses = tasks.getContent().stream().map(TaskResponse::from).toList();
        return new TaskPageResponse(
                taskResponses,
                tasks.getNumber(),
                tasks.getSize(),
                tasks.getTotalElements(),
                tasks.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID taskId) {
        return TaskResponse.from(findTask(taskId));
    }

    @Transactional
    public TaskResponse update(UUID taskId, UpdateTaskRequest request) {
        Task task = findTask(taskId);
        task.updateDetails(request.title(), request.description(), request.status(), request.priority());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID taskId, MatrizUserPrincipal caller) {
        Task task = findTask(taskId);
        Project project = requireProject(task.getProjectId());
        if (!task.getCreatorId().equals(caller.getUserId())
                && !project.getOwnerId().equals(caller.getUserId())) {
            throw new TaskAccessDeniedException();
        }
        taskRepository.delete(task);
    }

    private Task findTask(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
