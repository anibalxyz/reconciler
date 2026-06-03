package com.anibalxyz.features.auth.api;

import com.anibalxyz.features.common.api.routing.RouteGroup;
import com.anibalxyz.features.common.api.routing.RouteRegistry;
import io.javalin.Javalin;

public class AuthRoutes implements RouteRegistry {
  private final AuthApi authApi;
  private final JwtMiddleware jwtMiddleware;

  public AuthRoutes(AuthApi authApi, JwtMiddleware jwtMiddleware) {
    this.authApi = authApi;
    this.jwtMiddleware = jwtMiddleware;
  }

  @Override
  public void register(Javalin server) {
    new RouteGroup("/api/auth", server)
        .post("/login", authApi::login)
        .post("/refresh", authApi::refresh)
        .post("/logout", authApi::logout);

    jwtMiddleware.register(server); // TODO: move to a separate module
  }
}
