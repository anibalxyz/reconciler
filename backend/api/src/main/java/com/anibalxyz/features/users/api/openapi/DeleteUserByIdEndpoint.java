package com.anibalxyz.features.users.api.openapi;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponseExamples;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public interface DeleteUserByIdEndpoint {

  @OpenApi(
      summary = "Delete a user by ID",
      operationId = "deleteUserById",
      path = "/users/{id}",
      methods = HttpMethod.DELETE,
      tags = {"Users"},
      security = @OpenApiSecurity(name = "bearerAuth"),
      pathParams = {
        @OpenApiParam(
            name = "id",
            type = Integer.class,
            description = "The unique identifier of the user to delete.",
            required = true,
            example = "1")
      },
      responses = {
        @OpenApiResponse(status = "204", description = "User deleted successfully."),
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
