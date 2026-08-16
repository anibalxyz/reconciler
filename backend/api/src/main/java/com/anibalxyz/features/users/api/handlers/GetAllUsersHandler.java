package com.anibalxyz.features.users.api.handlers;

import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.openapi.GetAllUsersEndpoint;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.application.GetAllUsers;
import com.anibalxyz.features.users.domain.User;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class GetAllUsersHandler implements Handler, GetAllUsersEndpoint {
  private final GetAllUsers getAllUsers;

  public GetAllUsersHandler(GetAllUsers getAllUsers) {
    this.getAllUsers = getAllUsers;
  }

  @Override
  public void handle(@NotNull Context ctx) {
      List<User> users = getAllUsers.execute();
      List<UserDetailResponse> usersList = users.stream().map(UserMapper::toDetailResponse).toList();
      CollectionResponse<UserDetailResponse> response = CollectionResponse.ofSinglePage(usersList);

      ctx.status(200).json(response);
  }

}
