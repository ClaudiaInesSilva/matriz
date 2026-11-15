package com.matriz.comments;

import com.matriz.projects.UserSummaryResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID commentId,
        UUID taskId,
        UserSummaryResponse author,
        String content,
        OffsetDateTime createdAt) {
}
