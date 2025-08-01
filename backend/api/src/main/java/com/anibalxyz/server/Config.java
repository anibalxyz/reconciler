package com.anibalxyz.server;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;

import java.util.Map;

public class Config {

    public static Javalin getServer() {
        return init(Javalin.create(Config::apply));
    }

    private static void apply(JavalinConfig config) {
        config.useVirtualThreads = true;
        config.router.ignoreTrailingSlashes = true;
        config.http.defaultContentType = "application/json; charset=utf-8";
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
            if ((System.getenv("APP_ENV").equals("development"))) {
                rule.anyHost();
            } else {
                // TODO: set real allowed hosts dynamically
                rule.allowHost("allowed.com");
            }
        }));
    }

    private static Javalin init(Javalin app) {
        app.exception(Exception.class, (e, ctx) -> {
            boolean isDev = System.getenv("APP_ENV").equals("development");
            ctx.status(500);
            ctx.json(Map.of("error", isDev ? e.getMessage() : "Internal Server Error"));
        });
        return app;
    }
}
