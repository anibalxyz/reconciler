package com.anibalxyz.server.config.environment;

import com.anibalxyz.persistence.DatabaseVariables;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApplicationConfiguration(AppEnvironmentSource env, DatabaseVariables database) {

  public Map<String, Object> toMap() {
    Map<String, Object> configSummary = new LinkedHashMap<>();
    configSummary.put("environment", env.APP_ENV());

    Map<String, Object> api = new LinkedHashMap<>();
    api.put("url", env.API_URL());
    api.put("port", env.API_PORT());
    api.put("corsOrigins", env.CORS_ALLOWED_ORIGINS());
    configSummary.put("api", api);

    Map<String, Object> databaseMap = new LinkedHashMap<>();
    databaseMap.put("url", database.url());
    databaseMap.put("user", database.user());
    configSummary.put("database", databaseMap);

    Map<String, Object> jwt = new LinkedHashMap<>();
    jwt.put("issuer", env.JWT_ISSUER());
    jwt.put("accessTokenExpirationMinutes", env.JWT_ACCESS_EXPIRATION_TIME_SECONDS() / 60);
    jwt.put("refreshTokenExpirationDays", env.JWT_REFRESH_EXPIRATION_TIME_DAYS().toDays());
    configSummary.put("jwt", jwt);

    Map<String, Object> auth = new LinkedHashMap<>();
    auth.put("bcryptLogRounds", env.BCRYPT_LOG_ROUNDS());
    auth.put("cookieSecure", env.AUTH_COOKIE_SECURE());
    auth.put("cookieDomain", env.AUTH_COOKIE_DOMAIN());
    auth.put("cookieSameSite", env.AUTH_COOKIE_SAMESITE());
    auth.put("cookiePath", env.AUTH_COOKIE_PATH());
    configSummary.put("auth", auth);

    return configSummary;
  }
}
