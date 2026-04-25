package com.anibalxyz.features.common.api;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
  /** A role for any user, including unauthenticated ones. */
  GUEST,
  /** A role for users who have successfully logged in (have a valid JWT). */
  AUTHENTICATED
}
