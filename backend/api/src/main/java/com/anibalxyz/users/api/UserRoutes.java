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
    new RouteGroup("/users", this.server)
        .get("", this.userController.getAllUsers)
        .post("", this.userController.createUser)
        .get("/{id}", this.userController.getUserById)
        .put("/{id}", this.userController.updateUser)
        .delete("/{id}", this.userController.deleteById);
  }
}
