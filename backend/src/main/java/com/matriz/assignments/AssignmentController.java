package com.matriz.assignments;

import com.matriz.projects.UserSummaryResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssignmentController {
    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/api/projects/{projectId}/eligible-assignees")
    public List<UserSummaryResponse> listEligibleAssignees(@PathVariable UUID projectId) {
        return assignmentService.listEligibleAssignees(projectId);
    }

    @GetMapping("/api/tasks/{taskId}/assignees")
    public List<UserSummaryResponse> listTaskAssignees(@PathVariable UUID taskId) {
        return assignmentService.listTaskAssignees(taskId);
    }

    @PostMapping("/api/tasks/{taskId}/assignees")
    public ResponseEntity<Void> assignUser(
            @PathVariable UUID taskId,
            @Valid @RequestBody AssignUserRequest request) {
        assignmentService.assignUser(taskId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/tasks/{taskId}/assignees/{userId}")
    public ResponseEntity<Void> removeAssignee(@PathVariable UUID taskId, @PathVariable UUID userId) {
        assignmentService.removeAssignee(taskId, userId);
        return ResponseEntity.noContent().build();
    }
}
