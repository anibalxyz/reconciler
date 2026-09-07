package com.anibalxyz.features.auth.api;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import java.time.Clock;
import java.time.Instant;

public class AuthCookieService {
  public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
  private final Config config;
  private final Clock clock;

  public AuthCookieService(Clock clock, Config config) {
    this.clock = clock;
    this.config = config;
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
            config.authCookiePath(),
            (int) maxAgeInSeconds,
            config.authCookieSecure(),
            true,
            config.authCookieDomain(),
            config.authCookieSamesite());

    ctx.cookie(cookie);
  }

  public String getRefreshTokenCookie(Context ctx) {
    return ctx.cookie(REFRESH_TOKEN_COOKIE);
  }

  public interface Config {
    Boolean authCookieSecure();

    String authCookieDomain();

    SameSite authCookieSamesite();

    String authCookiePath();
  }
}
