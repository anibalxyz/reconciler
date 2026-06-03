package com.anibalxyz.features.users.api;

import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.features.common.api.routing.RouteGroup;
import com.anibalxyz.features.common.api.routing.RouteRegistry;
import io.javalin.Javalin;

public class UserRoutes implements RouteRegistry {
  private final UserApi userApi;

  public UserRoutes(UserApi userApi) {
    this.userApi = userApi;
  }

  @Override
  public void register(Javalin server) {
    new RouteGroup("/api/users", server)
        .get(userApi::getAllUsers, Role.AUTHENTICATED)
        .post(userApi::createUser, Role.GUEST)
        .get("/{id}", userApi::getUserById, Role.AUTHENTICATED)
        .put("/{id}", userApi::updateUserById, Role.AUTHENTICATED)
        .delete("/{id}", userApi::deleteUserById, Role.AUTHENTICATED);
  }
}
