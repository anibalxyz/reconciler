package com.anibalxyz.server.config;

import static net.logstash.logback.argument.StructuredArguments.v;

import com.anibalxyz.core.AppEnv;
import com.anibalxyz.persistence.DatabaseVariables;
import com.anibalxyz.server.config.groups.ClockConfig;
import com.anibalxyz.server.config.groups.FeatureFlags;
import com.anibalxyz.server.config.groups.HttpServerConfig;
import com.anibalxyz.server.config.groups.SecurityConfig;
import com.anibalxyz.server.exception.ConfigurationException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfiguration {
  private static final Logger log = LoggerFactory.getLogger(ApplicationConfiguration.class);
  private final AppEnv appEnv;
  private final SecurityConfig security;
  private final HttpServerConfig httpServer;
  private final ClockConfig clock;
  private final DatabaseVariables database;
  private final FeatureFlags featureFlags;

  private ApplicationConfiguration(
      AppEnv appEnv,
      HttpServerConfig httpServer,
      ClockConfig clock,
      SecurityConfig security,
      DatabaseVariables database,
      FeatureFlags featureFlags) {
    this.appEnv = appEnv;
    this.httpServer = httpServer;
    this.clock = clock;
    this.security = security;
    this.database = database;
    this.featureFlags = featureFlags;
  }

  public static ApplicationConfiguration from(Function<String, String> callback) {
    ConfigValueReader reader = ConfigValueReader.from(callback);

    AppEnv appEnv = getAppEnvFromString(reader.read("APP_ENV"));
    SecurityConfig securityConfig = SecurityConfig.from(reader, appEnv);
    HttpServerConfig httpServerConfig = HttpServerConfig.from(reader, appEnv);
    ClockConfig clockConfig = ClockConfig.from(reader);
    FeatureFlags featureFlags = FeatureFlags.from(reader);
    DatabaseVariables databaseVariables = DatabaseVariables.from(reader);

    var result =
        new ApplicationConfiguration(
            appEnv, httpServerConfig, clockConfig, securityConfig, databaseVariables, featureFlags);

    logLoadedConfiguration(result, appEnv);
    return result;
  }

  /**
   * Parses a string value into an `AppEnv` enum. The comparison is case-insensitive.
   *
   * @param value The string value to parse (e.g., "dev", "prod", "test").
   * @return The corresponding `AppEnv` enum.
   * @throws ConfigurationException.NeededPropertyException if the value is null, blank, or does not
   *     match any valid environment.
   */
  private static AppEnv getAppEnvFromString(String value) {
    if (value == null || value.isBlank()) {
      throw new ConfigurationException.MissingProperty("APP_ENV");
    }
    try {
      return AppEnv.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException.InvalidProperty(
          "APP_ENV", "Available values are: " + Arrays.toString(AppEnv.values()));
    }
  }

  /** TODO: refactor to a helper class, so can be used in other places */
  private static void logLoadedConfiguration(ApplicationConfiguration result, AppEnv env) {
    String logMessage = "Configuration loaded";
    if (env.equals(AppEnv.TEST)) {
      logMessage = logMessage.concat(": {}");
    }
    log.info(logMessage, v("config", result.toMap()));
  }

  public AppEnv appEnv() {
    return appEnv;
  }

  public SecurityConfig security() {
    return security;
  }

  public HttpServerConfig httpServer() {
    return httpServer;
  }

  public ClockConfig clock() {
    return clock;
  }

  public DatabaseVariables database() {
    return database;
  }

  public FeatureFlags featureFlags() {
    return featureFlags;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> configSummary = new LinkedHashMap<>();
    configSummary.put("app_env", appEnv.toString());
    configSummary.put("datetime_config", clock.toMap());
    configSummary.put("security_config", security.toMap());
    configSummary.put("database", database.toMap());
    configSummary.put("feature_flags", featureFlags.toMap());
    return configSummary;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
