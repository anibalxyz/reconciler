package com.anibalxyz.features.auth.api;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import java.time.Clock;
import java.time.Instant;

public class AuthCookieService {
  public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
  private final Env env;
  private final Clock clock;

  public AuthCookieService(Clock clock, Env env) {
    this.clock = clock;
    this.env = env;
  }

  public static long secondsUntilExpiry(Instant expiryDate, Instant now) {
    return Math.max(0, expiryDate.getEpochSecond() - now.getEpochSecond());
  }

  public void clearRefreshTokenCookie(Context ctx) {
    setRefreshTokenCookie(ctx, "", 0L);
  }

  public void setRefreshTokenCookie(Context ctx, String refreshToken, Instant expiryDate) {
    long maxAgeInSeconds = secondsUntilExpiry(expiryDate, clock.instant());
    setRefreshTokenCookie(ctx, refreshToken, maxAgeInSeconds);
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

  public String getRefreshTokenCookie(Context ctx) {
    return ctx.cookie(REFRESH_TOKEN_COOKIE);
  }

  public interface Env {
    Boolean AUTH_COOKIE_SECURE();

    String AUTH_COOKIE_DOMAIN();

    SameSite AUTH_COOKIE_SAMESITE();

    String AUTH_COOKIE_PATH();
  }
}
