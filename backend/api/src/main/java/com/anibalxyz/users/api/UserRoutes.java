package com.anibalxyz.users.api;

import com.anibalxyz.server.routes.RouteGroup;
import com.anibalxyz.server.routes.RouteRegistry;
import io.javalin.Javalin;

public class UserRoutes extends RouteRegistry {
  private final UserController userController;

  public UserRoutes(Javalin server, UserController userController) {
    super(server);
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
