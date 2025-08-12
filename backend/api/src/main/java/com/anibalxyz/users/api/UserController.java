package com.anibalxyz.users.api;

import com.anibalxyz.users.api.in.UserCreateRequest;
import com.anibalxyz.users.api.in.UserUpdateRequest;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  public void getAllUsers(Context ctx) {
    List<User> users = this.userService.getAllUsers();
    List<UserDetailResponse> response = users.stream().map(UserMapper::toDetailResponse).toList();
    ctx.json(response);
  }

  public void getUserById(Context ctx) {
    int id = getParamId(ctx);
    this.userService
        .getUserById(id)
        .map(UserMapper::toDetailResponse)
        .ifPresentOrElse(
            ctx::json,
            () -> ctx.status(404).json(Map.of("error", "User with id " + id + " not found")));
  }

  public void createUser(Context ctx) {
    UserCreateRequest request =
        ctx.bodyValidator(UserCreateRequest.class)
            .check(r -> r.name() != null && !r.name().isBlank(), "Name is required")
            .check(r -> r.email() != null && !r.email().isBlank(), "Email is required")
            .check(r -> r.password() != null && !r.password().isBlank(), "Password is required")
            .get();

    try {
      ctx.status(201)
          .json(
              UserMapper.toCreateResponse(
                  this.userService.createUser(
                      request.name(), request.email(), request.password())));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", e.getMessage()));
    }
  }

  public void updateUser(Context ctx) {
    UserUpdateRequest userUpdateRequest = ctx.bodyAsClass(UserUpdateRequest.class);
    int id = getParamId(ctx);

    if (!userUpdateRequest.isValid()) {
      ctx.status(400).json(Map.of("error", "At least one attribute must be given"));
      return;
    }

    try {
      ctx.status(200)
          .json(UserMapper.toDetailResponse(this.userService.updateUser(id, userUpdateRequest)));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", e.getMessage()));
    } catch (EntityNotFoundException e) {
      ctx.status(404).json(Map.of("error", e.getMessage()));
    }
  }

  public void deleteById(Context ctx) {
    int id = getParamId(ctx);
    try {
      this.userService.deleteUserById(id);
      ctx.status(204);
    } catch (EntityNotFoundException e) {
      ctx.status(404).json(Map.of("error", e.getMessage()));
    }
  }

  private int getParamId(Context ctx) {
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow(e -> new BadRequestResponse("Invalid ID format. Must be a number."));
  }
}
