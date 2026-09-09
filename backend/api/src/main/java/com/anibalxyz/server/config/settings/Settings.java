package com.anibalxyz.server.config.settings;

import java.util.Map;

/**
 * A cohesive group of config values (e.g., feature flags, database settings).
 *
 * <p>Implementors must provide a {@code public static from(SettingsReader, ...)} factory, which
 * means that the method must accept at least the SettingsReader parameter, but may accept more.
 *
 * <p>NOTE: Implementations may also implement component contracts (e.g. {@code
 * JwtService.Settings}). If that coupling ever grows back toward a god object, the plan is:
 *
 * <p>(a) stop implementing every contract and let consumers narrow their own;
 *
 * <p>(b) use anonymous implementations for short contracts or drop them by flattening the values
 * into the consumer;
 *
 * <p>(c) create inner records per component in the dependency container for contracts of 3+ values.
 */
public interface Settings {

  /**
   * Describes this group as flat key/value pairs for startup diagnostics. Useful for logging.
   *
   * @return variable names to their loaded values
   */
  Map<String, Object> toMap();
}
