package com.anibalxyz.features.users.api.out;

import com.anibalxyz.features.common.api.out.response.success.SuccessResponse;
import io.javalin.openapi.OpenApiExample;

public record UserCreateResponse(
    @OpenApiExample("1") int id,
    @OpenApiExample("John Doe") String name,
    @OpenApiExample("john.doe@example.com") String email)
    implements SuccessResponse {}
