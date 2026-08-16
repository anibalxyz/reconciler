package com.anibalxyz.features.users.api.openapi;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponseExamples;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public interface GetAllUsersEndpoint {

  @OpenApi(
      summary = "Get all users",
      operationId = "getAllUsers",
      path = "/users",
      methods = HttpMethod.GET,
      tags = {"Users"},
      security = @OpenApiSecurity(name = "bearerAuth"),
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "A list of all users.",
            content = @OpenApiContent(from = UserDetailResponse.Collection.class)),
        @OpenApiResponse(
            status = "401",
            description = "Authentication information is missing or invalid.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.UNAUTHORIZED)),
        @OpenApiResponse(
            status = "500",
            description = "Internal server error.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.INTERNAL_SERVER_ERROR))
      })
  void handle(Context ctx);
}
