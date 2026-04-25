package com.anibalxyz.shared;

import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.environment.ConfigurationFactory;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides centralized constants for testing, including environment configuration and mock data.
 */
public class Constants {
  private static final Logger log = LoggerFactory.getLogger(Constants.class);
  public static ApplicationConfiguration APP_CONFIG;
  public static AppEnvironmentSource APP_ENV;
  private static boolean initialized;

  public static void init() {
    if (initialized) return;

    APP_CONFIG = ConfigurationFactory.loadForTest();
    APP_ENV = APP_CONFIG.env();

    initialized = true;
    log.info("Constants initialized: {}", APP_CONFIG);
  }

  public static final class Users {
    public static final String VALID_NAME = "John Doe";
    public static final String VALID_EMAIL = "valid@email.com";
    public static final String VALID_PASSWORD = "V4L|D_Passw0Rd";

    public static final User VALID_USER =
        new User(
            1,
            Name.of(VALID_NAME).getValue(),
            Email.of(VALID_EMAIL).getValue(),
            PasswordHash.generate(VALID_PASSWORD, APP_ENV.BCRYPT_LOG_ROUNDS()).getValue(),
            Instant.now(),
            Instant.now());
  }

  public static final class Auth {
    // TODO: generate a really valid one at startup
    public static final String VALID_JWT =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";
    public static final String VALID_REFRESH_TOKEN = "e4192c47-9649-48be-9f88-523240f45b6e";

    public enum Token {
      ACCESS,
      REFRESH
    }
  }
}
