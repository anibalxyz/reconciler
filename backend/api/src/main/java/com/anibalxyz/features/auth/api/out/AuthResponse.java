package com.anibalxyz.features.auth.api.out;

import com.anibalxyz.core.api.response.success.SuccessResponse;
import io.javalin.openapi.OpenApiExample;

public record AuthResponse(
    @OpenApiExample("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken)
    implements SuccessResponse {}
