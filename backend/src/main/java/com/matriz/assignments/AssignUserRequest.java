package com.matriz.assignments;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignUserRequest(@NotNull UUID userId) {
}
