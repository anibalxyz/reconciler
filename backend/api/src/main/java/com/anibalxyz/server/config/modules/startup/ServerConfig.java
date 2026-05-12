package com.anibalxyz.server.config.modules.startup;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for setting up essential server features like JSON serialization (with
 * Jackson), enabling CORS, and registering default content types. This configuration is applied
 * once when the server starts.
 */
public class ServerConfig extends StartupConfig {

  private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);
  private final ServerEnvironment env;

  public ServerConfig(JavalinConfig javalinConfig, ServerEnvironment env) {
    super(javalinConfig);
    this.env = env;
  }

  private static void overrideIpGetter(JavalinConfig config) {
    config.contextResolver.ip =
        ctx -> {
          String ip = ctx.header("X-Real-IP");
          return (ip != null && !ip.isBlank()) ? ip : ctx.req().getRemoteAddr();
        };
  }

  private static void configureJsonMapper(JavalinConfig config) {
    config.jsonMapper(
        new JavalinJackson()
            .updateMapper(
                mapper -> mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));
  }

  private static void configureCors(JavalinConfig config, String[] hosts) {
    if (hosts != null && hosts.length > 0) {
      config.bundledPlugins.enableCors(
          cors ->
              cors.addRule(
                  rule -> {
                    for (String h : hosts) {
                      rule.allowHost(h);
                    }
                    rule.allowCredentials = true;
                  }));
    } else {
      log.warn("CORS_ALLOWED_ORIGINS not set. CORS will reject all origins");
    }
  }

  @Override
  public void apply() {
    javalinConfig.useVirtualThreads = true;
    javalinConfig.router.ignoreTrailingSlashes = true;
    javalinConfig.jetty.modifyServer(server -> server.setStopTimeout(5_000)); // graceful shutdown
    javalinConfig.http.defaultContentType = "application/json; charset=utf-8";

    configureCors(javalinConfig, env.CORS_ALLOWED_ORIGINS());
    configureJsonMapper(javalinConfig);
    overrideIpGetter(javalinConfig);
  }
}
