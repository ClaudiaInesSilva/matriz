package com.matriz.assignments;

import java.util.UUID;

public class DuplicateTaskAssignmentException extends RuntimeException {
    public DuplicateTaskAssignmentException(UUID taskId, UUID userId) {
        super("User " + userId + " is already assigned to task " + taskId);
    }
}
