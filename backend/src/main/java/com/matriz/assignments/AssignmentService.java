package com.matriz.assignments;

import com.matriz.projects.ProjectNotFoundException;
import com.matriz.projects.ProjectRepository;
import com.matriz.projects.UserSummaryResponse;
import com.matriz.tasks.TaskNotFoundException;
import com.matriz.tasks.TaskRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public AssignmentService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            TaskAssignmentRepository assignmentRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listEligibleAssignees(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return userRepository.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listTaskAssignees(UUID taskId) {
        requireTask(taskId);
        List<TaskAssignment> assignments = assignmentRepository.findAllById_TaskId(taskId);
        List<UUID> userIds = assignments.stream().map(assignment -> assignment.getId().getUserId()).toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return assignments.stream()
                .map(assignment -> {
                    UUID userId = assignment.getId().getUserId();
                    User user = usersById.get(userId);
                    if (user == null) {
                        throw new IllegalStateException("Assigned user account is missing: " + userId);
                    }
                    return toSummary(user);
                })
                .toList();
    }

    @Transactional
    public void assignUser(UUID taskId, UUID userId) {
        requireTask(taskId);
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        TaskAssignmentId assignmentId = new TaskAssignmentId(taskId, userId);
        if (assignmentRepository.existsById(assignmentId)) {
            throw new DuplicateTaskAssignmentException(taskId, userId);
        }

        try {
            assignmentRepository.saveAndFlush(new TaskAssignment(assignmentId));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateTaskAssignmentException(taskId, userId);
        }
    }

    @Transactional
    public void removeAssignee(UUID taskId, UUID userId) {
        requireTask(taskId);
        TaskAssignmentId assignmentId = new TaskAssignmentId(taskId, userId);
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new TaskAssignmentNotFoundException(taskId, userId);
        }
        assignmentRepository.deleteById(assignmentId);
    }

    private void requireTask(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId(), user.getDisplayName());
    }
}
