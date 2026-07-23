package com.anibalxyz.features.auth.api;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.api.env.AuthApiEnvironment;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import io.javalin.http.*;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthController implements AuthApi {
  // TODO: make configurable via environment
  public static final int REFRESH_TOKEN_COOKIE_MAX_AGE_MULTIPLIER = 2;
  public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final AuthApiEnvironment env;
  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final Clock clock;

  public AuthController(
      AuthApiEnvironment authApiEnvironment,
      AuthService authService,
      RefreshTokenService refreshTokenService,
      Clock clock) {
    this.env = authApiEnvironment;
    this.authService = authService;
    this.refreshTokenService = refreshTokenService;
    this.clock = clock;
  }

  @Override
  public void login(Context ctx) {
    LoginCommand command = ctx.bodyAsClass(LoginRequest.class).toCommand();
    Result<AuthResult, AuthService.AuthenticateUserError> authResult =
        authService.authenticateUser(command);
    if (authResult.isFailure()) {
      throw new FailureSignal(authResult.getError());
    }
    AuthResult authResultValue = authResult.getValue();

    setRefreshTokenCookie(
        ctx,
        authResultValue.refreshToken().token(),
        authResultValue.refreshToken().secondsUntilExpiry(clock.instant())
            * REFRESH_TOKEN_COOKIE_MAX_AGE_MULTIPLIER);
    ctx.status(200).json(new AuthResponse(authResultValue.accessToken()));
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

    Result<AuthResult, AuthService.RefreshTokensError> authResult =
        authService.refreshTokens(refreshTokenFromCookie);

    if (authResult.isFailure()) {
      throw new FailureSignal(authResult.getError());
    }

    AuthResult authResultValue = authResult.getValue();

    setRefreshTokenCookie(
        ctx,
        authResultValue.refreshToken().token(),
        authResultValue.refreshToken().secondsUntilExpiry(clock.instant())
            * REFRESH_TOKEN_COOKIE_MAX_AGE_MULTIPLIER);
    ctx.status(200).json(new AuthResponse(authResultValue.accessToken()));
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
