package com.anibalxyz.features.auth.application;

import java.time.Duration;

public interface AuthEnvironment {
  String JWT_SECRET();

  String JWT_ISSUER();

  Duration JWT_EXPIRATION_TIME();
}
