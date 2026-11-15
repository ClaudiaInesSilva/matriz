package com.matriz.assignments;

import java.util.UUID;

public class TaskAssignmentNotFoundException extends RuntimeException {
    public TaskAssignmentNotFoundException(UUID taskId, UUID userId) {
        super("Assignment not found for task " + taskId + " and user " + userId);
    }
}
