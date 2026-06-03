package com.anibalxyz.server.config.modules.startup;

import io.javalin.config.JavalinConfig;

/**
 * Abstract base class for startup configuration modules that are applied to the {@link
 * JavalinConfig} object during the server initialization phase, before the Javalin server instance
 * is actually started. These configurations typically involve global settings, plugin
 * registrations, and other setup tasks.
 */
public interface StartupConfig {
  /** Applies the startup configuration logic to the Javalin configuration. */
  public abstract void apply(JavalinConfig javalinConfig);
}
