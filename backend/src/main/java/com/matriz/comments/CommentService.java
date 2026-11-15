package com.matriz.comments;

import com.matriz.projects.UserSummaryResponse;
import com.matriz.security.MatrizUserPrincipal;
import com.matriz.tasks.TaskNotFoundException;
import com.matriz.tasks.TaskRepository;
import com.matriz.users.User;
import com.matriz.users.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(
            TaskRepository taskRepository,
            CommentRepository commentRepository,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID taskId) {
        requireTask(taskId);
        List<Comment> comments = commentRepository.findAllByTaskId(taskId);
        List<UUID> authorIds = comments.stream().map(Comment::getAuthorId).distinct().toList();
        Map<UUID, User> authorsById = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return comments.stream()
                .map(comment -> toResponse(comment, requireAuthor(comment.getAuthorId(), authorsById)))
                .toList();
    }

    @Transactional
    public CommentResponse add(UUID taskId, CreateCommentRequest request, MatrizUserPrincipal caller) {
        requireTask(taskId);
        Comment comment = commentRepository.save(new Comment(
                UUID.randomUUID(),
                taskId,
                caller.getUserId(),
                request.content(),
                OffsetDateTime.now(ZoneOffset.UTC)));
        User author = userRepository.findById(caller.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user account is missing"));
        return toResponse(comment, author);
    }

    private void requireTask(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
    }

    private User requireAuthor(UUID authorId, Map<UUID, User> authorsById) {
        User author = authorsById.get(authorId);
        if (author == null) {
            throw new IllegalStateException("Comment author account is missing: " + authorId);
        }
        return author;
    }

    private CommentResponse toResponse(Comment comment, User author) {
        return new CommentResponse(
                comment.getId(),
                comment.getTaskId(),
                new UserSummaryResponse(author.getId(), author.getDisplayName()),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
