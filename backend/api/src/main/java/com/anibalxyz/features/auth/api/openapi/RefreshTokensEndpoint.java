package com.anibalxyz.features.auth.api.openapi;

import com.anibalxyz.features.auth.api.out.AuthErrorResponseExamples;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;

public interface RefreshTokensEndpoint {
  @OpenApi(
      summary = "Refresh access token",
      operationId = "refreshToken",
      path = "/auth/refresh",
      methods = HttpMethod.POST,
      tags = {"Authentication"},
      responses = {
        @OpenApiResponse(
            status = "200",
            description =
                "Token refreshed successfully. Returns new access token in body, new refresh token in HttpOnly cookie.",
            content = @OpenApiContent(from = AuthResponse.class)),
        @OpenApiResponse(
            status = "401",
            description = "Invalid or expired refresh token.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = AuthErrorResponseExamples.INVALID_CREDENTIALS)),
        @OpenApiResponse(
            status = "401",
            description = "Missing refresh token in cookie.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = AuthErrorResponseExamples.MISSING_REFRESH_TOKEN))
      })
  void handle(Context ctx);
}
