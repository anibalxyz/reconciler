package com.anibalxyz.features.system.api;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.server.config.modules.startup.StartupConfig;
import io.javalin.config.JavalinConfig;

public class SystemRoutes implements StartupConfig {
  private final SystemApi systemApi;

  public SystemRoutes(SystemApi systemApi) {
    this.systemApi = systemApi;
  }

  @Override
  public void apply(JavalinConfig cfg) {
    cfg.routes.apiBuilder(() -> path("/health", () -> get(systemApi::healthCheck, Role.GUEST)));
  }
}
