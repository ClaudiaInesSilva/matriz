package com.matriz.assignments;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentId> {
    List<TaskAssignment> findAllById_TaskId(UUID taskId);
}
