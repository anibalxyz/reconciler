package com.anibalxyz.users.api;

import com.anibalxyz.users.api.in.UserCreateRequest;
import com.anibalxyz.users.api.in.UserUpdateRequest;
import com.anibalxyz.users.api.out.UserCreateResponse;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.exception.EntityNotFoundException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.validation.ValidationException;

/**
 * Defines the API contract for user-related operations.
 *
 * <p>This interface uses {@link OpenApi} annotations to document the REST endpoints, serving as a
 * single source of truth for the API's specification. The {@link UserController} provides the
 * concrete implementation for these operations.
 */
public interface UserApi {

  @OpenApi(
      summary = "Get all users",
      operationId = "getAllUsers",
      path = "/users",
      methods = HttpMethod.GET,
      tags = {"Users"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = UserDetailResponse[].class))
      })
  void getAllUsers(Context ctx);

  @OpenApi(
      summary = "Get a user by ID",
      operationId = "getUserById",
      path = "/users/{id}",
      methods = HttpMethod.GET,
      tags = {"Users"},
      pathParams = {@OpenApiParam(name = "id", type = Integer.class, description = "The user ID")},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = UserDetailResponse.class)),
        @OpenApiResponse(status = "400", description = "Invalid ID format"),
        @OpenApiResponse(status = "404", description = "User not found")
      })
  void getUserById(Context ctx);

  @OpenApi(
      summary = "Create a new user",
      operationId = "createUser",
      path = "/users",
      methods = HttpMethod.POST,
      tags = {"Users"},
      requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = UserCreateRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "201",
            description = "User created successfully",
            content = @OpenApiContent(from = UserCreateResponse.class)),
        @OpenApiResponse(
            status = "400",
            description = "Invalid input data (e.g., duplicate email, missing fields)")
      })
  void createUser(Context ctx);

  @OpenApi(
      summary = "Update an existing user",
      operationId = "updateUserById",
      path = "/users/{id}",
      methods = HttpMethod.PUT,
      tags = {"Users"},
      pathParams = {@OpenApiParam(name = "id", type = Integer.class, description = "The user ID")},
      requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = UserUpdateRequest.class)),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = UserDetailResponse.class)),
        @OpenApiResponse(status = "400", description = "Invalid input data or empty payload"),
        @OpenApiResponse(status = "404", description = "User not found")
      })
  void updateUserById(Context ctx)
      throws IllegalArgumentException,
          ValidationException,
          EntityNotFoundException,
          BadRequestResponse;

  @OpenApi(
      summary = "Delete a user by ID",
      operationId = "deleteUserById",
      path = "/users/{id}",
      methods = HttpMethod.DELETE,
      tags = {"Users"},
      pathParams = {@OpenApiParam(name = "id", type = Integer.class, description = "The user ID")},
      responses = {
        @OpenApiResponse(status = "204", description = "User deleted successfully"),
        @OpenApiResponse(status = "404", description = "User not found")
      })
  void deleteUserById(Context ctx);
}
