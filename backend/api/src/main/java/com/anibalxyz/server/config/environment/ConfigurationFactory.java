package com.anibalxyz.server.config.environment;

import static net.logstash.logback.argument.StructuredArguments.v;

import com.anibalxyz.persistence.DatabaseVariables;
import com.anibalxyz.server.config.AppEnv;
import com.anibalxyz.server.exception.ConfigurationException.*;
import io.javalin.http.SameSite;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: add Hikari configuration from env variables
// TODO: all this stuff surely can be done declaratively e.g. using some POO pattern.
//       I'm thinking in Strategy pattern
// TODO: adding a env-identifier to each variable (or set of vars) may be useful
/**
 * A factory for creating application configuration from various sources.
 *
 * <p>This class centralizes the logic for loading configuration data, such as database credentials,
 * JWT settings, and other application-level settings. It can load settings from either system
 * environment variables (standard for containerized environments) or a {@code .env.*} file (for
 * local development), adapting to different runtime environments.
 */
public class ConfigurationFactory {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationFactory.class);

  private ConfigurationFactory() {}

  public static ApplicationConfiguration load(String[] args) {
    return ArgParser.find("--env-file", args)
        .map(Paths::get)
        .map(ConfigurationFactory::loadFromEnvFile)
        .orElseGet(ConfigurationFactory::loadFromEnv);
  }

  /**
   * Loads configuration from system environment variables. This is the standard method for
   * containerized environments like Docker, where variables are passed directly to the container.
   *
   * @return A new {@link ApplicationConfiguration} instance.
   */
  public static ApplicationConfiguration loadFromEnv() {
    log.info("Loading configuration from the system environment.");
    return loadEnvironmentVariables(System::getenv);
  }

  /**
   * Loads configuration from a {@code .env.{appEnv}} file from the project's root. This method is
   * intended for local development, allowing developers to manage environment variables in a file.
   *
   * @return A new {@link ApplicationConfiguration} instance.
   * @throws UnableToLoadDotenvFile if the specified .env file cannot be found or read.
   */
  public static ApplicationConfiguration loadFromEnvFile(Path path) {
    Objects.requireNonNull(path, "'path' cannot be null");

    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
    } catch (IOException e) {
      throw new UnableToLoadDotenvFile(e);
    }
    log.info("Loading configuration from .env file.");
    return loadEnvironmentVariables(props::getProperty);
  }

  /**
   * Core method that loads, parses, and validates all environment variables from the specified
   * source. It constructs the final {@link ApplicationConfiguration} object.
   *
   * @param callback A function that resolves an environment variable name to its value.
   * @return A fully populated {@link ApplicationConfiguration} instance.
   * @throws NeededPropertyException if a required environment variable is missing or invalid.
   */
  private static ApplicationConfiguration loadEnvironmentVariables(
      Function<String, String> callback) {
    AppEnv appEnv = AppEnv.parseFromString(getEnvVar("APP_ENV", callback));

    ZoneId systemTimezone = ZoneId.of(getEnvVar("SYSTEM_TIMEZONE", callback));
    String systemTimeOverrideString = getEnvVar("SYSTEM_TIME_OVERRIDE", callback, true);
    Instant systemTimeOverride =
        (systemTimeOverrideString == null || systemTimeOverrideString.isBlank())
            ? null
            : Instant.parse(systemTimeOverrideString);

    String dbName = getEnvVar("DB_NAME", callback);
    String dbUser = getEnvVar("DB_USER", callback);
    String dbPassword = getEnvVar("DB_PASSWORD", callback);
    String dbPort = getEnvVar("DB_PORT", callback);
    String dbHost = getEnvVar("DB_HOST", callback);
    String apiProtocol = getEnvVar("API_PROTOCOL", callback, true);
    if (apiProtocol == null || apiProtocol.isBlank()) {
      apiProtocol = appEnv == AppEnv.PROD ? "https" : "http";
      log.warn("API_PROTOCOL was not provided or blank. Defaulting to '{}'", apiProtocol);
    }
    String apiHost = getEnvVar("API_HOST", callback);
    int apiPort = Integer.parseInt(getEnvVar("API_PORT", callback));
    String apiPrefix = "/api";
    String serverUrl = apiProtocol + "://" + apiHost + ":" + apiPort;
    String apiUrl = serverUrl + apiPrefix;
    String apiPublicUrl = getEnvVar("API_PUBLIC_URL", callback, true);
    if (apiPublicUrl == null || apiPublicUrl.isBlank()) {
      apiPublicUrl = apiUrl;
    }

    String frontendProtocol =
        Optional.ofNullable(getEnvVar("FRONTEND_PROTOCOL", callback, true))
            .filter(s -> !s.isBlank())
            .orElse(appEnv == AppEnv.PROD ? "https" : "http");

    String corsOriginsRaw = getEnvVar("CORS_ALLOWED_ORIGINS", callback, true);
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

    String contactEmail = getEnvVar("CONTACT_EMAIL", callback);

    // JWT configuration
    String jwtSecret = getEnvVar("JWT_SECRET", callback);
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new InvalidProperty("JWT_SECRET", "Must not be null or empty");
    }
    byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new InvalidProperty("JWT_SECRET", "Must be at least 256 bits (32 bytes/characters)");
    }
    SecretKey jwtKey = Keys.hmacShaKeyFor(secretBytes);
    String jwtIssuer = getEnvVar("JWT_ISSUER", callback);
    long jwtAccessExpirationTimeMinutes =
        Duration.ofMinutes(
                Long.parseLong(getEnvVar("JWT_ACCESS_EXPIRATION_TIME_MINUTES", callback)))
            .toMinutes();
    long jwtAccessExpirationTimeSeconds = jwtAccessExpirationTimeMinutes * 60;
    Duration jwtRefreshExpirationTime =
        Duration.ofDays(Long.parseLong(getEnvVar("JWT_REFRESH_EXPIRATION_TIME_DAYS", callback)));

    int bcryptLogRounds = Integer.parseInt(getEnvVar("BCRYPT_LOG_ROUNDS", callback));

    String authCookieDomain = getEnvVar("AUTH_COOKIE_DOMAIN", callback, true);
    authCookieDomain = authCookieDomain.isBlank() ? null : authCookieDomain;
    Boolean authCookieSecure = appEnv == AppEnv.PROD;
    String authCookiePath = apiPrefix + getEnvVar("AUTH_COOKIE_PATH", callback);

    // TODO: use agnostic enum instead. Final consumer maps to Javalin's SameSite
    SameSite authCookieSameSite;
    try {
      authCookieSameSite =
          SameSite.valueOf(getEnvVar("AUTH_COOKIE_SAMESITE", callback).toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidProperty("AUTH_COOKIE_SAMESITE", "Available values are: NONE, STRICT, LAX");
    }

    // Feature Flags
    // TODO: add separate inner record for feature flags
    String swaggerEnabledRaw = getEnvVar("SWAGGER_ENABLED", callback, true);
    if (swaggerEnabledRaw == null || swaggerEnabledRaw.isBlank()) swaggerEnabledRaw = "false";
    Boolean swaggerEnabled = Boolean.parseBoolean(swaggerEnabledRaw);

    AppEnvironmentSource env =
        new AppEnvironmentSource(
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
            authCookiePath,
            swaggerEnabled);

    ApplicationConfiguration result =
        new ApplicationConfiguration(
            env, DatabaseVariables.generate(dbHost, dbPort, dbName, dbUser, dbPassword));

    log.info("Configuration loaded", v("config", result.toMap()));
    return result;
  }

  /**
   * Safely retrieves a configuration value from a given source.
   *
   * @param name The name of the configuration property to retrieve.
   * @param source A function that takes the property name and returns its value.
   * @param allowEmpty If true, allows null or blank values; if false, throws an exception.
   * @return The value of the configuration property.
   * @throws MissingProperty if {@code allowEmpty} is false and the property is missing or blank.
   */
  private static String getEnvVar(
      String name, Function<String, String> source, boolean allowEmpty) {
    String value = source.apply(name);
    if (!allowEmpty && (value == null || value.isBlank())) {
      throw new MissingProperty(name);
    }
    return value;
  }

  /**
   * Safely retrieves a required configuration value from a given source.
   *
   * <p>This method is a convenience wrapper for {@link #getEnvVar(String, Function, boolean)} with
   * {@code allowEmpty} set to false.
   *
   * @param name The name of the configuration property to retrieve.
   * @param source A function that provides the value based on the name.
   * @return The non-blank value of the configuration property.
   * @throws MissingProperty if the property is missing or blank.
   */
  private static String getEnvVar(String name, Function<String, String> source) {
    return getEnvVar(name, source, false);
  }
}
