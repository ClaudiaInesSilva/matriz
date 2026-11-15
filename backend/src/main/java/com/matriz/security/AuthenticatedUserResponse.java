package com.matriz.security;

import java.util.UUID;

public record AuthenticatedUserResponse(UUID userId, String username, String displayName) {
    static AuthenticatedUserResponse from(MatrizUserPrincipal principal) {
        return new AuthenticatedUserResponse(
                principal.getUserId(), principal.getUsername(), principal.getDisplayName());
    }
}
