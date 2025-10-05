package com.anibalxyz.server.routes;

import io.javalin.Javalin;

public abstract class RouteRegistry {
  protected Javalin server;

  public RouteRegistry(Javalin server) {
    this.server = server;
  }

  public abstract void register();
}
