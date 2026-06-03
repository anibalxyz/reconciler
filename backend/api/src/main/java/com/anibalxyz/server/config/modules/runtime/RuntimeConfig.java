package com.anibalxyz.server.config.modules.runtime;

import io.javalin.Javalin;

/**
 * Base class for runtime configuration modules that are applied to a running {@link Javalin} server
 * instance and typically involve setting up handlers, filters, or other components that interact
 * with the request lifecycle.
 */
public interface RuntimeConfig {
  /** Applies the runtime configuration logic to the Javalin server. */
  void apply(Javalin server);
}
