package com.anibalxyz.server.config.environment;

import static net.logstash.logback.argument.StructuredArguments.v;

import com.anibalxyz.persistence.DatabaseVariables;
import com.anibalxyz.server.config.AppEnv;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfiguration {
  private static final Logger log = LoggerFactory.getLogger(ApplicationConfiguration.class);
  private final AppEnvironmentSource env;
  private final DatabaseVariables database;
  private final FeatureFlags featureFlags;

  private ApplicationConfiguration(
      AppEnvironmentSource env, DatabaseVariables database, FeatureFlags featureFlags) {
    this.env = env;
    this.database = database;
    this.featureFlags = featureFlags;
  }

  public static ApplicationConfiguration from(Function<String, String> callback) {
    ConfigValueReader reader = ConfigValueReader.from(callback);

    // TODO: add appEnv as a top level config
    AppEnvironmentSource appEnvironmentSource = AppEnvironmentSource.from(reader);
    FeatureFlags featureFlags = FeatureFlags.from(reader);
    DatabaseVariables databaseVariables = DatabaseVariables.from(reader);

    var result =
        new ApplicationConfiguration(appEnvironmentSource, databaseVariables, featureFlags);

    logLoadedConfiguration(result, appEnvironmentSource.APP_ENV());
    return result;
  }

  /** TODO: refactor to a helper class, so can be used in other places */
  private static void logLoadedConfiguration(ApplicationConfiguration result, AppEnv env) {
    String logMessage = "Configuration loaded";
    if (env.equals(AppEnv.TEST)) {
      logMessage = logMessage.concat(": {}");
    }
    log.info(logMessage, v("config", result.toMap()));
  }

  public AppEnvironmentSource env() {
    return env;
  }

  public DatabaseVariables database() {
    return database;
  }

  public FeatureFlags featureFlags() {
    return featureFlags;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> configSummary = new LinkedHashMap<>();
    configSummary.put("environment", env.toMap());
    configSummary.put("database", database.toMap());
    configSummary.put("feature_flags", featureFlags.toMap());
    return configSummary;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
