package com.anibalxyz.features.auth.api;

import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.api.env.AuthApiEnvironment;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import io.javalin.http.*;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthController implements AuthApi {
  public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final AuthApiEnvironment env;
  private final Clock clock;
  private final AuthenticateUser authenticateUser;
  private final RefreshTokens refreshTokens;
  private final RefreshTokenService refreshTokenService;

  public AuthController(
      AuthApiEnvironment authApiEnvironment,
      Clock clock,
      AuthenticateUser authenticateUser,
      RefreshTokens refreshTokens,
      RefreshTokenService refreshTokenService) {
    this.env = authApiEnvironment;
    this.clock = clock;
    this.authenticateUser = authenticateUser;
    this.refreshTokens = refreshTokens;
    this.refreshTokenService = refreshTokenService;
  }

  public static long secondsUntilExpiry(Instant expiryDate, Instant now) {
    return Math.max(0, expiryDate.getEpochSecond() - now.getEpochSecond());
  }

  @Override
  public void login(Context ctx) {
    LoginCommand command = ctx.bodyAsClass(LoginRequest.class).toCommand();
    AuthResult authResult = authenticateUser.execute(command).orThrow(FailureSignal::new);

    setRefreshTokenCookie(
        ctx,
        authResult.refreshToken().value(),
        secondsUntilExpiry(authResult.refreshTokenExpiryDate(), clock.instant()));
    ctx.status(200).json(new AuthResponse(authResult.accessToken()));
  }

  @Override
  public void logout(Context ctx) {
    String refreshTokenFromCookie = ctx.cookie("refreshToken");

    refreshTokenService.revokeToken(refreshTokenFromCookie);

    emptyRefreshTokenCookie(ctx);

    ctx.status(204);
    log.info("User logged out");
  }

  @Override
  public void refresh(Context ctx) {
    String refreshTokenFromCookie = ctx.cookie("refreshToken");
    if (refreshTokenFromCookie == null || refreshTokenFromCookie.isBlank()) {
      throw new UnauthorizedResponse("Missing refresh token in cookie");
    }

    AuthResult authResult =
        refreshTokens.execute(refreshTokenFromCookie).orThrow(FailureSignal::new);

    setRefreshTokenCookie(
        ctx,
        authResult.refreshToken().value(),
        secondsUntilExpiry(authResult.refreshTokenExpiryDate(), clock.instant()));
    ctx.status(200).json(new AuthResponse(authResult.accessToken()));
  }

  private void emptyRefreshTokenCookie(Context ctx) {
    setRefreshTokenCookie(ctx, "", 0L);
  }

  private void setRefreshTokenCookie(Context ctx, String refreshToken, long maxAgeInSeconds) {
    Cookie cookie =
        new Cookie(
            REFRESH_TOKEN_COOKIE,
            refreshToken,
            env.AUTH_COOKIE_PATH(),
            (int) maxAgeInSeconds,
            env.AUTH_COOKIE_SECURE(),
            true,
            env.AUTH_COOKIE_DOMAIN(),
            env.AUTH_COOKIE_SAMESITE());

    ctx.cookie(cookie);
  }
}
