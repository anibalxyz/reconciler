package com.anibalxyz.features.auth.api;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.anibalxyz.features.auth.api.handlers.LoginHandler;
import com.anibalxyz.features.auth.api.handlers.LogoutHandler;
import com.anibalxyz.features.auth.api.handlers.RefreshTokensHandler;
import com.anibalxyz.server.config.modules.StartupConfig;
import io.javalin.config.JavalinConfig;

public class AuthRoutes implements StartupConfig {
  private final LoginHandler loginHandler;
  private final LogoutHandler logoutHandler;
  private final RefreshTokensHandler refreshTokensHandler;

  public AuthRoutes(
      LoginHandler loginHandler,
      LogoutHandler logoutHandler,
      RefreshTokensHandler refreshTokensHandler) {
    this.loginHandler = loginHandler;
    this.logoutHandler = logoutHandler;
    this.refreshTokensHandler = refreshTokensHandler;
  }

  public void apply(JavalinConfig cfg) {
    cfg.routes.apiBuilder(
        () ->
            path(
                "/api/auth",
                () -> {
                  post("/login", loginHandler);
                  post("/refresh", refreshTokensHandler);
                  post("/logout", logoutHandler);
                }));
  }
}
