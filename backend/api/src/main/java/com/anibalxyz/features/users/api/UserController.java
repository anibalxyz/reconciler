package com.anibalxyz.features.users.api;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.application.CreateUser;
import com.anibalxyz.features.users.application.DeleteUserById;
import com.anibalxyz.features.users.application.GetAllUsers;
import com.anibalxyz.features.users.application.GetUserById;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.domain.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import java.util.List;

public class UserController implements UserApi {
  private final GetAllUsers getAllUsers;
  private final GetUserById getUserById;
  private final CreateUser createUser;
  private final UpdateUserById updateUserById;
  private final DeleteUserById deleteUserById;

  public UserController(
      GetAllUsers getAllUsers,
      GetUserById getUserById,
      CreateUser createUser,
      UpdateUserById updateUserById,
      DeleteUserById deleteUserById) {
    this.getAllUsers = getAllUsers;
    this.getUserById = getUserById;
    this.createUser = createUser;
    this.updateUserById = updateUserById;
    this.deleteUserById = deleteUserById;
  }

  @Override
  public void getAllUsers(Context ctx) {
    List<User> users = getAllUsers.execute();
    List<UserDetailResponse> usersList = users.stream().map(UserMapper::toDetailResponse).toList();
    CollectionResponse<UserDetailResponse> response = CollectionResponse.ofSinglePage(usersList);

    ctx.status(200).json(response);
  }

  @Override
  public void getUserById(Context ctx) throws BadRequestResponse {
    int id = getParamId(ctx);
    User user = getUserById.execute(id).orThrow(FailureSignal::new);

    ctx.status(200).json(UserMapper.toDetailResponse(user));
  }

  @Override
  public void createUser(Context ctx) {
    UserCreateRequest request = ctx.bodyAsClass(UserCreateRequest.class);

    User user = createUser.execute(request.toCommand()).orThrow(FailureSignal::new);

    ctx.status(201).json(UserMapper.toCreateResponse(user));
  }

  @Override
  public void updateUserById(Context ctx) throws BadRequestResponse {
    int id = getParamId(ctx);

    UserUpdateRequest userUpdateRequest = ctx.bodyAsClass(UserUpdateRequest.class);
    User user =
        updateUserById.execute(id, userUpdateRequest.toCommand()).orThrow(FailureSignal::new);

    ctx.status(200).json(UserMapper.toDetailResponse(user));
  }

  @Override
  public void deleteUserById(Context ctx) throws BadRequestResponse {
    int id = getParamId(ctx);
    deleteUserById.execute(id).orThrow(FailureSignal::new);
    ctx.status(204);
  }

  /**
   * @throws BadRequestResponse if the ID is missing or not a valid integer.
   */
  private int getParamId(Context ctx) throws BadRequestResponse {
    // TODO: migrate to a personalized error (at the moment this is an edge case so it does not
    //       matter)
    return ctx.pathParamAsClass("id", Integer.class)
        .getOrThrow(e -> new BadRequestResponse("Invalid ID format. Must be a number."));
  }
}
