package com.anibalxyz.features.users.api;

import static io.javalin.apibuilder.ApiBuilder.*;

import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.features.users.api.handlers.*;
import com.anibalxyz.server.config.modules.StartupConfig;
import io.javalin.config.JavalinConfig;

public class UserRoutes implements StartupConfig {
  private final GetAllUsersHandler getAllUsersHandler;
  private final GetUserByIdHandler getUserByIdHandler;
  private final CreateUserHandler createUserHandler;
  private final UpdateUserByIdHandler updateUserByIdHandler;
  private final DeleteUserByIdHandler deleteUserByIdHandler;

  public UserRoutes(
      GetAllUsersHandler getAllUsersHandler,
      GetUserByIdHandler getUserByIdHandler,
      CreateUserHandler createUserHandler,
      UpdateUserByIdHandler updateUserByIdHandler,
      DeleteUserByIdHandler deleteUserByIdHandler) {
    this.getAllUsersHandler = getAllUsersHandler;
    this.getUserByIdHandler = getUserByIdHandler;
    this.createUserHandler = createUserHandler;
    this.updateUserByIdHandler = updateUserByIdHandler;
    this.deleteUserByIdHandler = deleteUserByIdHandler;
  }

  @Override
  public void apply(JavalinConfig cfg) {
    cfg.routes.apiBuilder(
        () ->
            path(
                "/api/users",
                () -> {
                  get(getAllUsersHandler, Role.AUTHENTICATED);
                  post(createUserHandler, Role.GUEST);
                  path(
                      "/{id}",
                      () -> {
                        get(getUserByIdHandler, Role.AUTHENTICATED);
                        put(updateUserByIdHandler, Role.AUTHENTICATED);
                        delete(deleteUserByIdHandler, Role.AUTHENTICATED);
                      });
                }));
  }
}
