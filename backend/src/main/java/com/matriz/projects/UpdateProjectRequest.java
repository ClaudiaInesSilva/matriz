package com.matriz.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 3, max = 150, message = "Project name must contain 3 to 150 characters")
        String name,
        @Size(max = 2000, message = "Project description must not exceed 2000 characters")
        String description) {}
