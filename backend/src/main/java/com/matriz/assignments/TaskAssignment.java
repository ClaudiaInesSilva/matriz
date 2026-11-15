package com.matriz.assignments;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {
    @EmbeddedId
    private TaskAssignmentId id;

    protected TaskAssignment() {
    }

    public TaskAssignment(TaskAssignmentId id) {
        this.id = id;
    }

    public TaskAssignmentId getId() {
        return id;
    }
}
