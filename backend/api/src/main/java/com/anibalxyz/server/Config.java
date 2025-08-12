package com.anibalxyz.server;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;

public class Config {
  public static void apply(JavalinConfig config) {
    config.useVirtualThreads = true;
    config.router.ignoreTrailingSlashes = true;
    config.http.defaultContentType = "application/json; charset=utf-8";
    config.bundledPlugins.enableCors(
        cors ->
            cors.addRule(
                rule -> {
                  if ((System.getenv("APP_ENV").equals("development"))) {
                    rule.anyHost();
                  } else {
                    // TODO: set real allowed hosts dynamically
                    rule.allowHost("allowed.com");
                  }
                }));
    config.jsonMapper(
        new JavalinJackson()
            .updateMapper(
                mapper -> {
                  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                }));
  }
}
