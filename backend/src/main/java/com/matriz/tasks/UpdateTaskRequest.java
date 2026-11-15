package com.matriz.tasks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank(message = "Task title is required")
        @Size(min = 3, max = 150, message = "Task title must contain 3 to 150 characters")
        String title,
        @Size(max = 2000, message = "Task description must not exceed 2000 characters")
        String description,
        @NotNull(message = "Task status is required")
        TaskStatus status,
        @NotNull(message = "Task priority is required")
        TaskPriority priority) {
    public UpdateTaskRequest {
        title = title == null ? null : title.strip();
    }
}
