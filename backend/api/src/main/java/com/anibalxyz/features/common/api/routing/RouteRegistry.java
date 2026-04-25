package com.anibalxyz.features.common.api.routing;

import io.javalin.Javalin;

/**
 * An abstract base class for modular route registration.
 *
 * <p>This pattern allows the main {@link com.anibalxyz.server.Application} to discover and register
 * all route modules without being tightly coupled to them.
 */
public abstract class RouteRegistry {
  protected Javalin server;

  public RouteRegistry(Javalin server) {
    this.server = server;
  }

  /**
   * Defines and registers all routes for this module.
   *
   * <p>Implementations of this method should contain the complete routing logic for a specific
   * features or resource (e.g., all endpoints under "/users"). This is typically done using {@link
   * RouteGroup} to organize endpoints under a common path prefix and map them to controller
   * handlers.
   */
  public abstract void register();
}
