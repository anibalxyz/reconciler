package com.anibalxyz.features.common.api.routing;

import com.anibalxyz.features.common.api.Role;
import io.javalin.Javalin;
import io.javalin.http.Handler;

/**
 * A utility for defining a group of routes under a common path prefix.
 *
 * <p><b>Note:</b> This is a temporary wrapper used until the routing logic is migrated to Javalin's
 * {@code ApiBuilder}.
 *
 * <p>This class provides a fluent API to chain route definitions (e.g., get, post), making the
 * registration of related endpoints more concise and readable. For example, all routes starting
 * with "/users" can be defined together.
 */
public class RouteGroup {
  private static final Role DEFAULT_ROLE = Role.GUEST;

  private final String basePath;
  private final Javalin server;

  public RouteGroup(String basePath, Javalin server) {
    this.basePath = basePath;
    this.server = server;
  }

  public RouteGroup get(String path, Handler handler, Role... roles) {
    server.get(basePath + path, handler, roles);
    return this;
  }

  public RouteGroup get(String path, Handler handler) {
    get(path, handler, DEFAULT_ROLE);
    return this;
  }

  public RouteGroup get(Handler handler, Role... roles) {
    get("", handler, roles);
    return this;
  }

  public RouteGroup get(Handler handler) {
    get("", handler);
    return this;
  }

  public RouteGroup post(String path, Handler handler, Role... roles) {
    server.post(basePath + path, handler, roles);
    return this;
  }

  public RouteGroup post(String path, Handler handler) {
    post(path, handler, DEFAULT_ROLE);
    return this;
  }

  public RouteGroup post(Handler handler, Role... roles) {
    post("", handler, roles);
    return this;
  }

  public RouteGroup post(Handler handler) {
    post("", handler);
    return this;
  }

  public RouteGroup put(String path, Handler handler, Role... roles) {
    server.put(basePath + path, handler, roles);
    return this;
  }

  public RouteGroup put(String path, Handler handler) {
    put(path, handler, DEFAULT_ROLE);
    return this;
  }

  public RouteGroup put(Handler handler, Role... roles) {
    put("", handler, roles);
    return this;
  }

  public RouteGroup put(Handler handler) {
    put("", handler);
    return this;
  }

  public RouteGroup delete(String path, Handler handler, Role... roles) {
    server.delete(basePath + path, handler, roles);
    return this;
  }

  public RouteGroup delete(String path, Handler handler) {
    delete(path, handler, DEFAULT_ROLE);
    return this;
  }

  public RouteGroup delete(Handler handler, Role... roles) {
    delete("", handler, roles);
    return this;
  }

  public RouteGroup delete(Handler handler) {
    delete("", handler);
    return this;
  }
}
