package com.anibalxyz.features.auth.api.openapi;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiResponse;

public interface LogoutEndpoint {

  @OpenApi(
      summary = "Log out user",
      operationId = "logout",
      path = "/auth/logout",
      methods = HttpMethod.POST,
      tags = {"Authentication"},
      responses = {
        @OpenApiResponse(
            status = "204",
            description = "Logout successful. The refresh token cookie is cleared.")
      })
  void handle(Context ctx);
}
