package com.matriz.tasks;

import java.util.UUID;

public record TaskResponse(
        UUID taskId,
        UUID projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority());
    }
}
