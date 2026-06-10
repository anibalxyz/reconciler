package com.anibalxyz.features.users.api;

import static io.javalin.apibuilder.ApiBuilder.*;

import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.server.config.modules.startup.StartupConfig;
import io.javalin.config.JavalinConfig;

public class UserRoutes implements StartupConfig {
  private final UserApi userApi;

  public UserRoutes(UserApi userApi) {
    this.userApi = userApi;
  }

  @Override
  public void apply(JavalinConfig cfg) {
    cfg.routes.apiBuilder(
        () ->
            path(
                "/api/users",
                () -> {
                  get(userApi::getAllUsers, Role.AUTHENTICATED);
                  post(userApi::createUser, Role.GUEST);
                  path(
                      "/{id}",
                      () -> {
                        get(userApi::getUserById, Role.AUTHENTICATED);
                        put(userApi::updateUserById, Role.AUTHENTICATED);
                        delete(userApi::deleteUserById, Role.AUTHENTICATED);
                      });
                }));
  }
}
