package com.anibalxyz.features.auth.application.env;

import com.anibalxyz.features.auth.application.AuthService;
import java.time.Duration;

/**
 * Defines the contract for providing authentication-related environment configuration.
 *
 * <p>This interface acts as a port for environment-dependent settings required by the {@link
 * AuthService}, such as the time-window authentication setting. It decouples the service from the
 * concrete source of configuration.
 */
public interface AuthEnvironment {
  /**
   * Returns the expiration time for refresh tokens.
   *
   * @return The refresh token expiration time.
   */
  Duration JWT_REFRESH_EXPIRATION_TIME_DAYS();
}
