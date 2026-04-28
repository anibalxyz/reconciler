package com.anibalxyz.features.auth.application.env;

import javax.crypto.SecretKey;

public interface JwtEnvironment {
  SecretKey JWT_KEY();

  String JWT_ISSUER();

  long JWT_ACCESS_EXPIRATION_TIME_MINUTES();
}
