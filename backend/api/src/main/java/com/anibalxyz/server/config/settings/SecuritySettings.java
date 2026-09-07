package com.anibalxyz.server.config.settings;

import com.anibalxyz.core.AppEnv;
import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.users.application.CreateUser;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.server.config.SettingsReader;
import com.anibalxyz.server.exception.ConfigurationException;
import io.javalin.http.SameSite;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.jspecify.annotations.NonNull;

public class SecuritySettings
    implements Settings,
        UpdateUserById.Settings,
        CreateUser.Settings,
        AuthenticateUser.Settings,
        RefreshTokens.Settings,
        AuthCookieService.Settings,
        JwtService.Settings {
  private final int bcryptLogRounds;
  private final SecretKey jwtKey;
  private final String jwtIssuer;
  private final long jwtAccessExpirationTimeSeconds;
  private final Duration jwtRefreshExpirationTimeDays;
  private final Boolean authCookieSecure;
  private final String authCookieDomain;
  private final SameSite authCookieSameSite;
  private final String authCookiePath;

  private SecuritySettings(
      int bcryptLogRounds,
      SecretKey jwtKey,
      String jwtIssuer,
      long jwtAccessExpirationTimeSeconds,
      Duration jwtRefreshExpirationTimeDays,
      Boolean authCookieSecure,
      String authCookieDomain,
      SameSite authCookieSameSite,
      String authCookiePath) {

    this.bcryptLogRounds = bcryptLogRounds;
    this.jwtKey = jwtKey;
    this.jwtIssuer = jwtIssuer;
    this.jwtAccessExpirationTimeSeconds = jwtAccessExpirationTimeSeconds;
    this.jwtRefreshExpirationTimeDays = jwtRefreshExpirationTimeDays;
    this.authCookieSecure = authCookieSecure;
    this.authCookieDomain = authCookieDomain;
    this.authCookieSameSite = authCookieSameSite;
    this.authCookiePath = authCookiePath;
  }

  public static SecuritySettings from(SettingsReader reader, AppEnv appEnv) {

    // JWT configuration
    String jwtSecret = reader.read("JWT_SECRET");
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new ConfigurationException.InvalidProperty("JWT_SECRET", "Must not be null or empty");
    }
    byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new ConfigurationException.InvalidProperty(
          "JWT_SECRET", "Must be at least 256 bits (32 bytes/characters)");
    }
    SecretKey jwtKey = Keys.hmacShaKeyFor(secretBytes);
    String jwtIssuer = reader.read("JWT_ISSUER");
    long jwtAccessExpirationTimeMinutes =
        Duration.ofMinutes(Long.parseLong(reader.read("JWT_ACCESS_EXPIRATION_TIME_MINUTES")))
            .toMinutes();
    long jwtAccessExpirationTimeSeconds = jwtAccessExpirationTimeMinutes * 60;
    Duration jwtRefreshExpirationTime =
        Duration.ofDays(Long.parseLong(reader.read("JWT_REFRESH_EXPIRATION_TIME_DAYS")));

    int bcryptLogRounds = Integer.parseInt(reader.read("BCRYPT_LOG_ROUNDS"));

    // Auth cookie configuration
    String authCookieDomain = reader.read("AUTH_COOKIE_DOMAIN", true);
    authCookieDomain =
        authCookieDomain == null || authCookieDomain.isBlank() ? null : authCookieDomain;
    Boolean authCookieSecure = appEnv == AppEnv.PROD;
    String authCookiePath = "/api" + reader.read("AUTH_COOKIE_PATH");

    SameSite authCookieSameSite;
    try {
      authCookieSameSite = SameSite.valueOf(reader.read("AUTH_COOKIE_SAMESITE").toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException.InvalidProperty(
          "AUTH_COOKIE_SAMESITE", "Available values are: NONE, STRICT, LAX");
    }

    return new SecuritySettings(
        bcryptLogRounds,
        jwtKey,
        jwtIssuer,
        jwtAccessExpirationTimeSeconds,
        jwtRefreshExpirationTime,
        authCookieSecure,
        authCookieDomain,
        authCookieSameSite,
        authCookiePath);
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> configMap = new LinkedHashMap<>();
    configMap.put("bcrypt_log_rounds", bcryptLogRounds);
    configMap.put("jwt_issuer", jwtIssuer);
    configMap.put(
        "jwt_access_expiration_time_minutes",
        Duration.ofSeconds(jwtAccessExpirationTimeSeconds).toMinutes());
    configMap.put("jwt_refresh_expiration_time_days", jwtRefreshExpirationTimeDays.toDays());
    configMap.put("auth_cookie_secure", authCookieSecure);
    configMap.put("auth_cookie_domain", authCookieDomain);
    configMap.put("auth_cookie_same_site", authCookieSameSite.getValue().split("=")[1]);
    configMap.put("auth_cookie_path", authCookiePath);
    return configMap;
  }

  @Override
  public @NonNull String toString() {
    return toMap().toString();
  }

  @Override
  public int bcryptLogRounds() {
    return bcryptLogRounds;
  }

  @Override
  public SecretKey jwtKey() {
    return jwtKey;
  }

  @Override
  public String jwtIssuer() {
    return jwtIssuer;
  }

  @Override
  public long jwtAccessExpirationTimeSeconds() {
    return jwtAccessExpirationTimeSeconds;
  }

  @Override
  public Duration jwtRefreshExpirationTimeDays() {
    return jwtRefreshExpirationTimeDays;
  }

  @Override
  public Boolean authCookieSecure() {
    return authCookieSecure;
  }

  @Override
  public String authCookieDomain() {
    return authCookieDomain;
  }

  @Override
  public SameSite authCookieSamesite() {
    return authCookieSameSite;
  }

  @Override
  public String authCookiePath() {
    return authCookiePath;
  }
}
