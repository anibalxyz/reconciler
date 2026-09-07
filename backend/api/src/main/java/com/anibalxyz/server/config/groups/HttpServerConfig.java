package com.anibalxyz.server.config.groups;

import com.anibalxyz.core.AppEnv;
import com.anibalxyz.server.config.ConfigValueReader;
import com.anibalxyz.server.config.modules.ServerConfig;
import com.anibalxyz.server.config.modules.SwaggerConfig;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerConfig implements ConfigGroup, ServerConfig.Config, SwaggerConfig.Config {
  private static final Logger log = LoggerFactory.getLogger(HttpServerConfig.class);
  private final String serverUrl;
  private final String apiUrl;
  private final int apiPort;
  private final String apiPublicUrl;
  private final String[] corsAllowedOrigins;
  private final String contactEmail;

  private HttpServerConfig(
      String serverUrl,
      String apiUrl,
      int apiPort,
      String apiPublicUrl,
      String[] corsAllowedOrigins,
      String contactEmail) {
    this.serverUrl = serverUrl;
    this.apiUrl = apiUrl;
    this.apiPort = apiPort;
    this.apiPublicUrl = apiPublicUrl;
    this.corsAllowedOrigins = corsAllowedOrigins;
    this.contactEmail = contactEmail;
  }

  public static HttpServerConfig from(ConfigValueReader reader, AppEnv appEnv) {
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

    return new HttpServerConfig(
        serverUrl, apiUrl, apiPort, apiPublicUrl, corsAllowedOrigins, contactEmail);
  }

  public String apiUrl() {
    return apiUrl;
  }

  public String serverUrl() {
    return serverUrl;
  }

  public String contactEmail() {
    return contactEmail;
  }

  public String apiPublicUrl() {
    return apiPublicUrl;
  }

  public int apiPort() {
    return apiPort;
  }

  public String[] corsAllowedOrigins() {
    return corsAllowedOrigins;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> configMap = new LinkedHashMap<>();

    configMap.put("server_url", serverUrl);
    configMap.put("api_url", apiUrl);
    configMap.put("api_port", apiPort);
    configMap.put("api_public_url", apiPublicUrl);
    configMap.put("cors_allowed_origins", corsAllowedOrigins);
    configMap.put("contact_email", contactEmail);

    return configMap;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
