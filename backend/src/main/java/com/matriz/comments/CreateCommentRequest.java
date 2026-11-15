package com.matriz.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(@NotBlank(message = "Comment content must not be blank")
                                   @Size(min = 1, max = 2000, message = "Comment content must be between 1 and 2000 characters")
                                   String content) {
}
