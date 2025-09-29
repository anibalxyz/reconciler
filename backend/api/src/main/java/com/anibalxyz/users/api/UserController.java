package com.anibalxyz.users.api;

import com.anibalxyz.users.api.in.UserCreateRequest;
import com.anibalxyz.users.api.in.UserUpdateRequest;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.validation.ValidationException;
import java.util.List;

public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  public void getAllUsers(Context ctx) {
    List<User> users = userService.getAllUsers();
    List<UserDetailResponse> response = users.stream().map(UserMapper::toDetailResponse).toList();
    ctx.status(200).json(response);
  }

  public void getUserById(Context ctx) throws BadRequestResponse, EntityNotFoundException {
    int id = getParamId(ctx);
    ctx.status(200).json(UserMapper.toDetailResponse(userService.getUserById(id)));
  }

  public void createUser(Context ctx) throws IllegalArgumentException, ValidationException {
    UserCreateRequest request =
        ctx.bodyValidator(UserCreateRequest.class)
            .check(r -> r.name() != null && !r.name().isBlank(), "Name is required")
            .check(r -> r.email() != null && !r.email().isBlank(), "Email is required")
            .check(r -> r.password() != null && !r.password().isBlank(), "Password is required")
            .get();

    ctx.status(201).json(UserMapper.toCreateResponse(userService.createUser(request)));
  }

  public void updateUserById(Context ctx)
      throws IllegalArgumentException,
          ValidationException,
          EntityNotFoundException,
          BadRequestResponse {
    int id = getParamId(ctx);

    String badRequestMessage = "At least one field (name, email, password) must be provided";

    UserUpdateRequest userUpdateRequest =
        ctx.bodyValidator(UserUpdateRequest.class)
            .check(UserUpdateRequest::hasAtLeastOneField, badRequestMessage)
            .get();

    ctx.status(200)
        .json(UserMapper.toDetailResponse(userService.updateUserById(id, userUpdateRequest)));
  }

  public void deleteUserById(Context ctx) throws EntityNotFoundException, BadRequestResponse {
    userService.deleteUserById(getParamId(ctx));
    ctx.status(204);
  }

  private int getParamId(Context ctx) throws BadRequestResponse {
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow(e -> new BadRequestResponse("Invalid ID format. Must be a number."));
  }
}
