package com.anibalxyz.features.users.api.openapi;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponseExamples;
import com.anibalxyz.features.users.api.in.UpdateUserRequest;
import com.anibalxyz.features.users.api.out.DetailedUserResponse;
import com.anibalxyz.features.users.api.out.UsersErrorResponseExamples;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public interface UpdateUserByIdEndpoint {

  @OpenApi(
      summary = "Update an existing user",
      operationId = "updateUserById",
      path = "/users/{id}",
      methods = HttpMethod.PUT,
      tags = {"Users"},
      security = @OpenApiSecurity(name = "bearerAuth"),
      pathParams = {
        @OpenApiParam(
            name = "id",
            type = Integer.class,
            description = "The unique identifier of the user to update.",
            required = true,
            example = "1")
      },
      requestBody =
          @OpenApiRequestBody(
              description = "The user data to update. At least one field must be provided.",
              required = true,
              content = @OpenApiContent(from = UpdateUserRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "200",
            description = "User updated successfully.",
            content = @OpenApiContent(from = DetailedUserResponse.class)),
        @OpenApiResponse(
            status = "401",
            description = "Authentication information is missing or invalid.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.UNAUTHORIZED)),
        @OpenApiResponse(
            status = "400",
            description = "Invalid input provided, such as empty payload, or duplicate email.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = UsersErrorResponseExamples.UPDATE_USER_BAD_REQUEST)),
        @OpenApiResponse(
            status = "404",
            description = "User with the specified ID not found.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = ErrorResponseExamples.RESOURCE_NOT_FOUND)),
        @OpenApiResponse(
            status = "409",
            description = "Conflict: email already in use.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = UsersErrorResponseExamples.EMAIL_ALREADY_IN_USE))
      })
  void handle(Context ctx);
}
