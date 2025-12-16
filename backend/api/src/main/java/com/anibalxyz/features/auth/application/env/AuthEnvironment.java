package com.anibalxyz.features.auth.application.env;

import com.anibalxyz.features.auth.application.AuthService;

/**
 * Defines the contract for providing authentication-related environment configuration.
 *
 * <p>This interface acts as a port for environment-dependent settings required by the {@link
 * AuthService}, such as the time-window authentication setting. It decouples the service from the
 * concrete source of configuration.
 */
public interface AuthEnvironment {
  /**
   * Returns whether the authentication time-window feature is enabled.
   *
   * @return {@code true} if the time-window is enabled, {@code false} otherwise.
   */
  boolean AUTH_ENABLE_TIME_WINDOW();
}
