package com.anibalxyz.features.auth.api.openapi;

import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthErrorResponseExamples;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public interface LoginEndpoint {

  @OpenApi(
      summary = "Authenticate user and get access token",
      operationId = "login",
      path = "/auth/login",
      methods = HttpMethod.POST,
      tags = {"Authentication"},
      requestBody =
          @OpenApiRequestBody(
              description = "User credentials for login",
              required = true,
              content = @OpenApiContent(from = LoginRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "200",
            description =
                "Authentication successful. Returns access token in body, refresh token in HttpOnly cookie.",
            content = @OpenApiContent(from = AuthResponse.class)),
        @OpenApiResponse(
            status = "400",
            description = "Invalid input (e.g., missing email/password)",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = AuthErrorResponseExamples.INVALID_INPUT_PROVIDED)),
        @OpenApiResponse(
            status = "401",
            description = "Invalid credentials",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = AuthErrorResponseExamples.INVALID_CREDENTIALS))
      })
  void handle(Context ctx);
}
