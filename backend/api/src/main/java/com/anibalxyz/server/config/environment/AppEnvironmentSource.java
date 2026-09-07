package com.anibalxyz.server.config.environment;

import com.anibalxyz.features.auth.api.AuthCookieService;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.users.application.CreateUser;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.config.modules.ServerConfig;
import com.anibalxyz.server.exception.ConfigurationException;
import io.javalin.http.SameSite;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single source of truth for application configuration.
 *
 * <p>Implements multiple feature-specific interfaces to provide decoupled settings to different
 * modules without exposing the entire environment.
 */
public record AppEnvironmentSource(
    AppEnv APP_ENV,
    ZoneId SYSTEM_TIMEZONE,
    Instant SYSTEM_TIME_OVERRIDE,
    String SERVER_URL,
    String API_URL,
    int API_PORT,
    String API_PUBLIC_URL,
    String[] CORS_ALLOWED_ORIGINS,
    String CONTACT_EMAIL,
    int BCRYPT_LOG_ROUNDS,
    SecretKey JWT_KEY,
    String JWT_ISSUER,
    long JWT_ACCESS_EXPIRATION_TIME_SECONDS,
    Duration JWT_REFRESH_EXPIRATION_TIME_DAYS,
    Boolean AUTH_COOKIE_SECURE,
    String AUTH_COOKIE_DOMAIN,
    SameSite AUTH_COOKIE_SAMESITE,
    String AUTH_COOKIE_PATH)
    implements ConfigGroup,
        UpdateUserById.Env,
        CreateUser.Env,
        ServerConfig.Env,
        AuthenticateUser.Env,
        RefreshTokens.Env,
        AuthCookieService.Env,
        JwtService.Env {

  private static final Logger log = LoggerFactory.getLogger(AppEnvironmentSource.class);

  public static AppEnvironmentSource from(ConfigValueReader reader) {
    AppEnv appEnv = AppEnv.parseFromString(reader.read("APP_ENV"));

    ZoneId systemTimezone = ZoneId.of(reader.read("SYSTEM_TIMEZONE"));
    String systemTimeOverrideString = reader.read("SYSTEM_TIME_OVERRIDE", true);
    Instant systemTimeOverride =
        (systemTimeOverrideString == null || systemTimeOverrideString.isBlank())
            ? null
            : Instant.parse(systemTimeOverrideString);

    String apiProtocol = reader.read("API_PROTOCOL", true);
    if (apiProtocol == null || apiProtocol.isBlank()) {
      apiProtocol = appEnv == AppEnv.PROD ? "https" : "http";
      log.warn("API_PROTOCOL was not provided or blank. Defaulting to '{}'", apiProtocol);
    }
    String apiHost = reader.read("API_HOST");
    int apiPort = Integer.parseInt(reader.read("API_PORT"));
    String apiPrefix = "/api";
    String serverUrl = apiProtocol + "://" + apiHost + ":" + apiPort;
    String apiUrl = serverUrl + apiPrefix;
    String apiPublicUrl = reader.read("API_PUBLIC_URL", true);
    if (apiPublicUrl == null || apiPublicUrl.isBlank()) {
      apiPublicUrl = apiUrl;
    }

    String frontendProtocol =
        Optional.ofNullable(reader.read("FRONTEND_PROTOCOL", true))
            .filter(s -> !s.isBlank())
            .orElse(appEnv == AppEnv.PROD ? "https" : "http");

    String corsOriginsRaw = reader.read("CORS_ALLOWED_ORIGINS", true);
    String[] corsAllowedOrigins;
    if (corsOriginsRaw == null || corsOriginsRaw.isBlank()) {
      corsAllowedOrigins = new String[0];
    } else {
      corsAllowedOrigins =
          Arrays.stream(corsOriginsRaw.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .map(origin -> frontendProtocol + "://" + origin)
              .toArray(String[]::new);
    }

    String contactEmail = reader.read("CONTACT_EMAIL");

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

    String authCookieDomain = reader.read("AUTH_COOKIE_DOMAIN", true);
    authCookieDomain =
        authCookieDomain == null || authCookieDomain.isBlank() ? null : authCookieDomain;
    Boolean authCookieSecure = appEnv == AppEnv.PROD;
    String authCookiePath = apiPrefix + reader.read("AUTH_COOKIE_PATH");

    SameSite authCookieSameSite;
    try {
      authCookieSameSite = SameSite.valueOf(reader.read("AUTH_COOKIE_SAMESITE").toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException.InvalidProperty(
          "AUTH_COOKIE_SAMESITE", "Available values are: NONE, STRICT, LAX");
    }

    return new AppEnvironmentSource(
        appEnv,
        systemTimezone,
        systemTimeOverride,
        serverUrl,
        apiUrl,
        apiPort,
        apiPublicUrl,
        corsAllowedOrigins,
        contactEmail,
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
    Map<String, Object> envMap = new LinkedHashMap<>();
    envMap.put("app_env", APP_ENV.toString());
    envMap.put("system_timezone", SYSTEM_TIMEZONE.toString());
    envMap.put("system_time_override", SYSTEM_TIME_OVERRIDE);
    envMap.put("server_url", SERVER_URL);
    envMap.put("api_url", API_URL);
    envMap.put("api_port", API_PORT);
    envMap.put("api_public_url", API_PUBLIC_URL);
    envMap.put("cors_allowed_origins", CORS_ALLOWED_ORIGINS);
    envMap.put("contact_email", CONTACT_EMAIL);
    envMap.put("bcrypt_log_rounds", BCRYPT_LOG_ROUNDS);
    envMap.put("jwt_issuer", JWT_ISSUER);
    envMap.put(
        "jwt_access_expiration_time_minutes",
        Duration.ofSeconds(JWT_ACCESS_EXPIRATION_TIME_SECONDS).toMinutes());
    envMap.put("jwt_refresh_expiration_time_days", JWT_REFRESH_EXPIRATION_TIME_DAYS.toDays());
    envMap.put("auth_cookie_secure", AUTH_COOKIE_SECURE);
    envMap.put("auth_cookie_domain", AUTH_COOKIE_DOMAIN);
    envMap.put("auth_cookie_same_site", AUTH_COOKIE_SAMESITE.getValue().split("=")[1]);
    envMap.put("auth_cookie_path", AUTH_COOKIE_PATH);
    return envMap;
  }

  @Override
  public @NonNull String toString() {
    return toMap().toString();
  }
}
