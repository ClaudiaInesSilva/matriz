package com.matriz.projects;

import java.util.UUID;

public record ProjectResponse(UUID projectId, String name, String description, UserSummaryResponse owner) {}
