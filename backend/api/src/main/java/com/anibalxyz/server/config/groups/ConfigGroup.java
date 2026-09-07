package com.anibalxyz.server.config.groups;

import java.util.Map;

/**
 * A cohesive group of config values (e.g., feature flags, database settings).
 *
 * <p>Implementors must provide a {@code public static from(ConfigValueReader, ...)} factory, which
 * means that the method must accept at least the ConfigValueReader parameter, but may accept more.
 */
public interface ConfigGroup {

  /**
   * Describes this group as flat key/value pairs for startup diagnostics. Useful for logging.
   *
   * @return variable names to their loaded values
   */
  Map<String, Object> toMap();
}
