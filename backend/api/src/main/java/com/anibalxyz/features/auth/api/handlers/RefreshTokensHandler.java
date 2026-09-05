package com.anibalxyz.features.auth.api.handlers;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.api.exception.MissingRefreshTokenCookie;
import com.anibalxyz.features.auth.api.openapi.RefreshTokensEndpoint;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.auth.application.out.AuthResult;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jspecify.annotations.NonNull;

public class RefreshTokensHandler implements Handler, RefreshTokensEndpoint {
  private final AuthCookieService authCookieService;
  private final RefreshTokens refreshTokens;

  public RefreshTokensHandler(AuthCookieService authCookieService, RefreshTokens refreshTokens) {
    this.authCookieService = authCookieService;
    this.refreshTokens = refreshTokens;
  }

  @Override
  public void handle(@NonNull Context ctx) {
    String refreshTokenFromCookie = authCookieService.getRefreshTokenCookie(ctx);
    if (refreshTokenFromCookie == null || refreshTokenFromCookie.isBlank()) {
      throw new MissingRefreshTokenCookie();
    }

    AuthResult authResult =
        refreshTokens.execute(refreshTokenFromCookie).orThrow(FailureSignal::new);

    authCookieService.setRefreshTokenCookie(
        ctx, authResult.refreshToken().value(), authResult.refreshTokenExpiryDate());
    ctx.status(200).json(new AuthResponse(authResult.accessToken()));
  }
}
