package com.matriz.tasks;

public class TaskAccessDeniedException extends RuntimeException {
    public TaskAccessDeniedException() {
        super("Only the task creator or project owner may delete this task");
    }
}
