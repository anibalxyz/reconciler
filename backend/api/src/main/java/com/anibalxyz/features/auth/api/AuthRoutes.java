package com.anibalxyz.features.auth.api;

import com.anibalxyz.features.common.api.routing.RouteGroup;
import com.anibalxyz.features.common.api.routing.RouteRegistry;
import io.javalin.Javalin;

public class AuthRoutes extends RouteRegistry {
  private final AuthApi authApi;
  private final JwtMiddleware jwtMiddleware;

  public AuthRoutes(Javalin server, AuthApi authApi, JwtMiddleware jwtMiddleware) {
    super(server);
    this.authApi = authApi;
    this.jwtMiddleware = jwtMiddleware;
  }

  @Override
  public void register() {
    new RouteGroup("/api/auth", server)
        .post("/login", authApi::login)
        .post("/refresh", authApi::refresh)
        .post("/logout", authApi::logout);

    jwtMiddleware.register(); // TODO: move to a separate module
  }
}
