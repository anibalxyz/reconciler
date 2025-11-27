package com.anibalxyz.features.auth.api;

import io.javalin.http.SameSite;

public interface AuthApiEnvironment {
  Boolean AUTH_COOKIE_SECURE();

  String AUTH_COOKIE_DOMAIN();

  SameSite AUTH_COOKIE_SAMESITE();

  String AUTH_COOKIE_PATH();
}
