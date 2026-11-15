package com.matriz.tasks;

import java.util.List;

public record TaskPageResponse(
        List<TaskResponse> tasks,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
