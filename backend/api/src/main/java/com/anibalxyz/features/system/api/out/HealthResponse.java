package com.anibalxyz.features.system.api.out;

import com.anibalxyz.features.common.api.out.response.success.SuccessResponse;
import io.javalin.openapi.OpenApiExample;

public record HealthResponse(@OpenApiExample("true") boolean dbIsConnected)
    implements SuccessResponse {}
