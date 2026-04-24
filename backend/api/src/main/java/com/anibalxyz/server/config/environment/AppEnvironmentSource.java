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
import javax.crypto.SecretKey;

/**
 * A concrete implementation that holds all application-level environment variables.
 *
 * <p>This record provides a type-safe, immutable representation of settings loaded from the
 * environment (e.g., .env files or system variables), acting as the single source of truth for
 * configuration. It implements various {@code ...Environment} interfaces to provide decoupled,
 * feature-specific configurations to different parts of the application.
 */
public record AppEnvironmentSource(
    AppEnv APP_ENV,
    ZoneId SYSTEM_TIMEZONE,
    Instant SYSTEM_TIME_OVERRIDE,
    String SERVER_URL,
    String API_URL,
    int API_PORT,
    String API_PREFIX,
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
    String AUTH_COOKIE_PATH)
    implements UsersEnvironment,
        ServerEnvironment,
        JwtEnvironment,
        AuthApiEnvironment,
        AuthEnvironment {}
  // TODO: implement toString() method to output a prettier string and to hide sensitive data
