package com.matriz.assignments;

import com.matriz.projects.ApiErrorResponse;
import com.matriz.projects.ProjectNotFoundException;
import com.matriz.tasks.TaskNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = AssignmentController.class)
public class AssignmentExceptionHandler {
    @ExceptionHandler({ProjectNotFoundException.class, TaskNotFoundException.class, UserNotFoundException.class,
            TaskAssignmentNotFoundException.class})
    ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({DuplicateTaskAssignmentException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception) {
        String message = exception instanceof DuplicateTaskAssignmentException
                ? exception.getMessage()
                : "User is already assigned to the task";
        return error(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    ResponseEntity<ApiErrorResponse> badRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Assignment request validation failed")
                : "Assignment request is invalid";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message));
    }
}
