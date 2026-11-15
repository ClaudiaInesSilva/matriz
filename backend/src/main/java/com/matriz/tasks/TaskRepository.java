package com.matriz.tasks;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    boolean existsByProjectId(UUID projectId);

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    Page<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    Page<Task> findByProjectIdAndPriority(UUID projectId, TaskPriority priority, Pageable pageable);

    Page<Task> findByProjectIdAndStatusAndPriority(
            UUID projectId, TaskStatus status, TaskPriority priority, Pageable pageable);
}
