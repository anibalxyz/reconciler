package com.anibalxyz.features.users.api.openapi;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponseExamples;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public interface GetUserByIdEndpoint {

  @OpenApi(
      summary = "Get a user by ID",
      operationId = "getUserById",
      path = "/users/{id}",
      methods = HttpMethod.GET,
      tags = {"Users"},
      security = @OpenApiSecurity(name = "bearerAuth"),
      pathParams = {
        @OpenApiParam(
            name = "id",
            type = Integer.class,
            description = "The unique identifier of the user.",
            required = true,
            example = "1")
      },
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "Successfully retrieved the user.",
            content = @OpenApiContent(from = UserDetailResponse.class)),
        @OpenApiResponse(
            status = "401",
            description = "Authentication information is missing or invalid.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.UNAUTHORIZED)),
        @OpenApiResponse(
            status = "400",
            description = "Invalid ID format supplied.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.INVALID_ID)),
        @OpenApiResponse(
            status = "404",
            description = "User with the specified ID not found.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.RESOURCE_NOT_FOUND))
      })
  void handle(Context ctx);
}
