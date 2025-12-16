package com.anibalxyz.features.auth.api.env;

import io.javalin.http.SameSite;

/**
 * Defines the contract for providing authentication API-related environment configuration.
 *
 * <p>This interface acts as a port for environment-dependent settings required by the {@link
 * com.anibalxyz.features.auth.api.AuthController}, such as cookie settings. It decouples the
 * controller from the concrete source of configuration.
 */
public interface AuthApiEnvironment {
  /**
   * Returns whether the authentication cookie should be marked as secure (HTTPS only).
   *
   * @return {@code true} if the cookie is secure, {@code false} otherwise.
   */
  Boolean AUTH_COOKIE_SECURE();

  /**
   * Returns the domain for which the authentication cookie is valid.
   *
   * @return The cookie domain.
   */
  String AUTH_COOKIE_DOMAIN();

  /**
   * Returns the SameSite policy for the authentication cookie.
   *
   * @return The SameSite policy.
   */
  SameSite AUTH_COOKIE_SAMESITE();

  /**
   * Returns the path for which the authentication cookie is valid.
   *
   * @return The cookie path.
   */
  String AUTH_COOKIE_PATH();
}
