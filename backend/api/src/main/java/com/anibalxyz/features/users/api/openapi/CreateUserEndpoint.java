package com.anibalxyz.features.users.api.openapi;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.api.in.CreateUserRequest;
import com.anibalxyz.features.users.api.out.CreateUserResponse;
import com.anibalxyz.features.users.api.out.UsersErrorResponseExamples;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public interface CreateUserEndpoint {

  @OpenApi(
      summary = "Create a new user",
      operationId = "createUser",
      path = "/users",
      methods = HttpMethod.POST,
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(
              description = "The user to create.",
              required = true,
              content = @OpenApiContent(from = CreateUserRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "201",
            description = "User created successfully.",
            content = @OpenApiContent(from = CreateUserResponse.class)),
        @OpenApiResponse(
            status = "400",
            description =
                "Invalid input provided, such as a duplicate email, missing fields, or invalid password format.",
            content =
                @OpenApiContent(
                    from = ErrorResponse.class,
                    example = UsersErrorResponseExamples.CREATE_USER_BAD_REQUEST)),
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
