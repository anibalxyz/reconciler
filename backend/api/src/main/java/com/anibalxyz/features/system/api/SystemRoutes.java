package com.anibalxyz.features.system.api;

import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.features.common.api.routing.RouteGroup;
import com.anibalxyz.features.common.api.routing.RouteRegistry;
import io.javalin.Javalin;

public class SystemRoutes extends RouteRegistry {

  private final SystemApi systemApi;

  public SystemRoutes(Javalin server, SystemApi systemApi) {
    super(server);
    this.systemApi = systemApi;
  }

  @Override
  public void register() {
    new RouteGroup("/health", server).get(systemApi::healthCheck, Role.GUEST);
  }
}
