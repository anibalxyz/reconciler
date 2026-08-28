package com.anibalxyz.features.auth.api;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.anibalxyz.server.config.modules.StartupConfig;
import io.javalin.config.JavalinConfig;

public class AuthRoutes implements StartupConfig {
  private final AuthApi authApi;

  public AuthRoutes(AuthApi authApi) {
    this.authApi = authApi;
  }

  public void apply(JavalinConfig cfg) {
    cfg.routes.apiBuilder(
        () ->
            path(
                "/api/auth",
                () -> {
                  post("/login", authApi::login);
                  post("/refresh", authApi::refresh);
                  post("/logout", authApi::logout);
                }));
  }
}
