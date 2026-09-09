package com.anibalxyz.features.auth.api;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import java.time.Clock;
import java.time.Instant;

public class AuthCookieService {
  public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
  private final Settings settings;
  private final Clock clock;

  public AuthCookieService(Clock clock, Settings settings) {
    this.clock = clock;
    this.settings = settings;
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
            settings.authCookiePath(),
            (int) maxAgeInSeconds,
            settings.authCookieSecure(),
            true,
            settings.authCookieDomain(),
            settings.authCookieSamesite());

    ctx.cookie(cookie);
  }

  public String getRefreshTokenCookie(Context ctx) {
    return ctx.cookie(REFRESH_TOKEN_COOKIE);
  }

  public interface Settings {
    Boolean authCookieSecure();

    String authCookieDomain();

    SameSite authCookieSamesite();

    String authCookiePath();
  }
}
