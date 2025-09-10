package com.anibalxyz.users.api;

import com.anibalxyz.server.RouteGroup;
import io.javalin.Javalin;

public class UserRoutes {
  private final Javalin server;
  private final UserController userController;

  public UserRoutes(Javalin server, UserController userController) {
    this.server = server;
    this.userController = userController;
  }

  public void register() {
    new RouteGroup("/users", server)
        .get("", userController::getAllUsers)
        .post("", userController::createUser)
        .get("/{id}", userController::getUserById)
        .put("/{id}", userController::updateUserById)
        .delete("/{id}", userController::deleteUserById);
  }
}
