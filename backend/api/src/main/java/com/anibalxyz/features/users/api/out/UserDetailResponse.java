package com.anibalxyz.features.users.api.out;

import io.javalin.openapi.OpenApiExample;
import java.time.Instant;

// TODO: improve semantics
public record UserDetailResponse(
    @OpenApiExample("1") int id,
    @OpenApiExample("John Doe") String name,
    @OpenApiExample("john.doe@example.com") String email,
    @OpenApiExample("2025-10-10T10:00:00") Instant createdAt,
    @OpenApiExample("2025-10-10T10:00:00") Instant updatedAt) {}
