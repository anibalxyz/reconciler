package com.anibalxyz.features.system.api;

import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.features.common.api.routing.RouteGroup;
import com.anibalxyz.features.common.api.routing.RouteRegistry;
import io.javalin.Javalin;

public class SystemRoutes implements RouteRegistry {
  private final SystemApi systemApi;

  public SystemRoutes(SystemApi systemApi) {
    this.systemApi = systemApi;
  }

  @Override
  public void register(Javalin server) {
    new RouteGroup("/health", server).get(systemApi::healthCheck, Role.GUEST);
  }
}
