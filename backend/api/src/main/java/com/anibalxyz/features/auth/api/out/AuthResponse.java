package com.anibalxyz.features.auth.api.out;

import io.javalin.openapi.OpenApiExample;

public record AuthResponse(
    @OpenApiExample("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken) {}
