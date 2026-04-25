package com.anibalxyz.features.system.api.out;

import io.javalin.openapi.OpenApiExample;

public record HealthResponse(@OpenApiExample("true") boolean dbIsConnected) {}
