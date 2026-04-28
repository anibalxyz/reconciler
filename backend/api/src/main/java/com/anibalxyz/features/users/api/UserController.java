package com.anibalxyz.features.users.api;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.common.application.exception.FailureSignal;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import java.util.List;

public class UserController implements UserApi {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void getAllUsers(Context ctx) {
    List<User> users = userService.getAllUsers();
    List<UserDetailResponse> response = users.stream().map(UserMapper::toDetailResponse).toList();
    ctx.status(200).json(response);
  }

  @Override
  public void getUserById(Context ctx) throws BadRequestResponse {
    int id = getParamId(ctx);
    Result<User, UserNotFoundError> result = userService.getUserById(id);
    if (result.isFailure()) {
      throw new FailureSignal(result.getError());
    }

    ctx.status(200).json(UserMapper.toDetailResponse(result.getValue()));
  }

  @Override
  public void createUser(Context ctx) {
    UserCreateRequest request = ctx.bodyAsClass(UserCreateRequest.class);

    Result<User, ValidationNotification<UserDomainError>> result =
        userService.createUser(request.toCommand());

    if (result.isFailure()) {
      throw new FailureSignal(result.getError());
    }

    ctx.status(201).json(UserMapper.toCreateResponse(result.getValue()));
  }

  @Override
  public void updateUserById(Context ctx) throws BadRequestResponse {
    int id = getParamId(ctx);

    UserUpdateRequest userUpdateRequest = ctx.bodyAsClass(UserUpdateRequest.class);
    Result<User, UserService.UpdateUserByIdError> result =
        userService.updateUserById(id, userUpdateRequest.toCommand());

    if (result.isFailure()) {
      throw new FailureSignal(result.getError());
    }

    ctx.status(200).json(UserMapper.toDetailResponse(result.getValue()));
  }

  @Override
  public void deleteUserById(Context ctx) throws BadRequestResponse {
    int id = getParamId(ctx);
    Result<Void, UserNotFoundError> result = userService.deleteUserById(id);
    if (result.isFailure()) {
      throw new FailureSignal(result.getError());
    }
    ctx.status(204);
  }

  /**
   * @throws BadRequestResponse if the ID is missing or not a valid integer.
   */
  private int getParamId(Context ctx) throws BadRequestResponse {
    // TODO: migrate to a personalized error (at the moment this is a edge case so it does not
    //       matter)
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow(e -> new BadRequestResponse("Invalid ID format. Must be a number."));
  }
}
