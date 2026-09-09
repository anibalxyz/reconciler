package com.anibalxyz.features.system.api.out;

import com.anibalxyz.core.api.response.success.SuccessResponse;
import io.javalin.openapi.OpenApiExample;

public record HealthResponse(@OpenApiExample("true") boolean dbIsConnected)
    implements SuccessResponse {}
