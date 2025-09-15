package com.anibalxyz.server.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;

public class InitConfig implements ServerConfig {

  private final JavalinConfig javalinConfig;
  private final AppConfig config;

  public InitConfig(JavalinConfig javalinConfig, AppConfig config) {
    this.javalinConfig = javalinConfig;
    this.config = config;
  }

  @Override
  public void apply() {
    javalinConfig.useVirtualThreads = true;
    javalinConfig.router.ignoreTrailingSlashes = true;
    javalinConfig.jetty.modifyServer(server -> server.setStopTimeout(5_000)); // graceful shutdown
    javalinConfig.http.defaultContentType = "application/json; charset=utf-8";
    javalinConfig.bundledPlugins.enableCors(
        cors ->
            cors.addRule(
                rule -> {
                  if ((config.env().equals("development"))) {
                    rule.anyHost();
                  } else {
                    rule.allowHost("allowed.com");
                  }
                }));
    javalinConfig.jsonMapper(
        new JavalinJackson()
            .updateMapper(
                mapper -> {
                  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                }));
  }
}
