package com.anibalxyz.features.auth.api;

import com.anibalxyz.server.routes.RouteGroup;
import com.anibalxyz.server.routes.RouteRegistry;
import io.javalin.Javalin;

public class AuthRoutes extends RouteRegistry {
  private final AuthController authController;

  public AuthRoutes(Javalin server, AuthController authController) {
    super(server);
    this.authController = authController;
  }

  @Override
  public void register() {
    new RouteGroup("/auth", server).post("", authController::login);
  }
}
