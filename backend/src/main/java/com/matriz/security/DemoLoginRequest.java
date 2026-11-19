package com.matriz.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DemoLoginRequest(
        @NotBlank
        @Pattern(regexp = "^(demo\\.alex|demo\\.sam)$", message = "Invalid demo username")
        String username) {}
