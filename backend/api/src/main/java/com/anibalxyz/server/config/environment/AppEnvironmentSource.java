package com.anibalxyz.server.config.environment;

import com.anibalxyz.features.auth.api.env.AuthApiEnvironment;
import com.anibalxyz.features.auth.application.env.AuthEnvironment;
import com.anibalxyz.features.auth.application.env.JwtEnvironment;
import com.anibalxyz.features.users.application.env.UsersEnvironment;
import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.config.modules.startup.ServerEnvironment;
import io.javalin.http.SameSite;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import javax.crypto.SecretKey;
import org.jspecify.annotations.NonNull;

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
    long JWT_ACCESS_EXPIRATION_TIME_MINUTES,
    Duration JWT_REFRESH_EXPIRATION_TIME_DAYS,
    Boolean AUTH_COOKIE_SECURE,
    String AUTH_COOKIE_DOMAIN,
    SameSite AUTH_COOKIE_SAMESITE,
    String AUTH_COOKIE_PATH,
    Boolean SWAGGER_ENABLED)
    implements UsersEnvironment,
        ServerEnvironment,
        JwtEnvironment,
        AuthApiEnvironment,
        AuthEnvironment {
  @Override
  public @NonNull String toString() {
    return "AppEnvironmentSource["
        + "APP_ENV="
        + APP_ENV
        + ", API_PORT="
        + API_PORT
        + ", API_URL="
        + API_URL
        + ", CORS_ALLOWED_ORIGINS="
        + Arrays.toString(CORS_ALLOWED_ORIGINS)
        + ", JWT_KEY=******"
        + ", JWT_ISSUER="
        + JWT_ISSUER
        + ", BCRYPT_LOG_ROUNDS="
        + BCRYPT_LOG_ROUNDS
        + "]";
  }
}
