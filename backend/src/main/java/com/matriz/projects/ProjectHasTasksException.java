package com.matriz.projects;

public class ProjectHasTasksException extends RuntimeException {
    public ProjectHasTasksException() {
        super("Project cannot be deleted while it contains tasks");
    }
}
