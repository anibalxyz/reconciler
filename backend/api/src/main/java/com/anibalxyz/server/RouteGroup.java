package com.anibalxyz.server;

import io.javalin.Javalin;
import io.javalin.http.Handler;

public class RouteGroup {
  private final String basePath;
  private final Javalin server;

  public RouteGroup(String basePath, Javalin server) {
    this.basePath = basePath;
    this.server = server;
  }

  public RouteGroup get(String path, Handler handler) {
    server.get(basePath + path, handler);
    return this;
  }

  public RouteGroup post(String path, Handler handler) {
    server.post(basePath + path, handler);
    return this;
  }

  public RouteGroup put(String path, Handler handler) {
    server.put(basePath + path, handler);
    return this;
  }

  public RouteGroup delete(String path, Handler handler) {
    server.delete(basePath + path, handler);
    return this;
  }
}
