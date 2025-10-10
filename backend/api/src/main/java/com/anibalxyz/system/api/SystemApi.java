package com.anibalxyz.system.api;

import com.anibalxyz.system.api.out.HealthResponse;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;

public interface SystemApi {

    @OpenApi(
        summary = "Health check",
        operationId = "healthCheck",
        path = "/health",
        methods = HttpMethod.GET,
        tags = {"System"},
        responses = {
            @OpenApiResponse(
                status = "200",
                description = "System status.",
                content = @OpenApiContent(from = HealthResponse.class)
            )
        }
    )
    void healthCheck(Context ctx);
}
