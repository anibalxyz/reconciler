package com.anibalxyz.features;

import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.environment.ConfigurationFactory;
import java.time.Duration;
import java.time.Instant;

public class Constants {
  public static ApplicationConfiguration APP_CONFIG;
  private static boolean initialized;

  public static void init() {
    if (initialized) return;

    APP_CONFIG = ConfigurationFactory.loadForTest();
    Environment.init(APP_CONFIG.env());

    initialized = true;
  }

  // Could be accessed using an "AppEnvironmentSource env" property,
  // but this way is more comfortable to use
  public static final class Environment {
    public static int BCRYPT_LOG_ROUNDS;
    public static String JWT_SECRET;
    public static String JWT_ISSUER;
    public static Duration JWT_ACCESS_EXPIRATION_TIME_MINUTES;
    public static Duration JWT_REFRESH_EXPIRATION_TIME_DAYS;

    static void init(AppEnvironmentSource env) {
      BCRYPT_LOG_ROUNDS = env.BCRYPT_LOG_ROUNDS();
      JWT_SECRET = env.JWT_SECRET();
      JWT_ISSUER = env.JWT_ISSUER();
      JWT_ACCESS_EXPIRATION_TIME_MINUTES = env.JWT_ACCESS_EXPIRATION_TIME_MINUTES();
      JWT_REFRESH_EXPIRATION_TIME_DAYS = env.JWT_REFRESH_EXPIRATION_TIME_DAYS();
    }
  }

  public static final class Users {
    public static final String VALID_NAME = "John Doe";
    public static final String VALID_EMAIL = "valid@email.com";
    public static final String VALID_PASSWORD = "V4L|D_Passw0Rd";

    public static final User VALID_USER =
        new User(
            1,
            VALID_NAME,
            new Email(VALID_EMAIL),
            PasswordHash.generate(VALID_PASSWORD, Environment.BCRYPT_LOG_ROUNDS),
            Instant.now(),
            Instant.now());
  }

  public static final class Auth {
    public static final String VALID_JWT =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";
    public static final String VALID_REFRESH_TOKEN = "e4192c47-9649-48be-9f88-523240f45b6e";
  }
}
