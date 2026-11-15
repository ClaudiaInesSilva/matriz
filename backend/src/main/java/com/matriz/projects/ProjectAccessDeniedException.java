package com.matriz.projects;

public class ProjectAccessDeniedException extends RuntimeException {
    public ProjectAccessDeniedException() {
        super("Only the project owner may update or delete this project");
    }
}
