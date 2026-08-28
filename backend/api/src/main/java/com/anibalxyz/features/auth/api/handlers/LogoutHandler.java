package com.anibalxyz.features.auth.api.handlers;

import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.api.openapi.LogoutEndpoint;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutHandler implements Handler, LogoutEndpoint {
  private static final Logger log = LoggerFactory.getLogger(LogoutHandler.class);
  private final AuthCookieService authCookieService;
  private final RefreshTokenService refreshTokenService;

  public LogoutHandler(
      AuthCookieService authCookieService, RefreshTokenService refreshTokenService) {
    this.authCookieService = authCookieService;
    this.refreshTokenService = refreshTokenService;
  }

  @Override
  public void handle(@NotNull Context ctx) {
    String refreshTokenFromCookie = authCookieService.getRefreshTokenCookie(ctx);

    refreshTokenService.revokeToken(refreshTokenFromCookie);

    authCookieService.clearRefreshTokenCookie(ctx);

    ctx.status(204);
    log.info("User logged out");
  }
}
