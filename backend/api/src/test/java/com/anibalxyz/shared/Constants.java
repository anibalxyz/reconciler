package com.anibalxyz.shared;

import static com.anibalxyz.shared.Constants.Users.VALID_USER;

import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.TokenHash;
import com.anibalxyz.features.users.domain.*;
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
    public static final Integer VALID_USER_ID_INT = 1;
    public static final String VALID_NAME_STRING = "John Doe";
    public static final String VALID_EMAIL_STRING = "valid@email.com";
    public static final String VALID_PASSWORD_STRING = "V4L|D_Passw0Rd";

    public static final UserId VALID_USER_ID = ResultAsserts.success(UserId.of(VALID_USER_ID_INT));
    public static final Name VALID_NAME = ResultAsserts.success(Name.of(VALID_NAME_STRING));
    public static final Email VALID_EMAIL = ResultAsserts.success(Email.of(VALID_EMAIL_STRING));
    public static final Password VALID_PASSWORD =
        ResultAsserts.success(Password.of(VALID_PASSWORD_STRING));
    public static final PasswordHash VALID_PASSWORD_HASH =
        PasswordHash.of(VALID_PASSWORD, APP_ENV.BCRYPT_LOG_ROUNDS());

    /**
     * A pre-built user whose credentials match the VALID_* constants
     *
     * <p>Ideal for tests that need a user whose password is known since the plaintext cannot be
     * read back from the entity.
     */
    public static final User VALID_USER = buildUser(1);

    public static User buildUser(int id, String email) {
      return User.reconstitute(
          ResultAsserts.success(UserId.of(id)),
          VALID_NAME,
          ResultAsserts.success(Email.of(email)),
          VALID_PASSWORD_HASH,
          Instant.now(),
          Instant.now());
    }

    public static User buildUser(int id) {
      return User.reconstitute(
          ResultAsserts.success(UserId.of(id)),
          VALID_NAME,
          VALID_EMAIL,
          VALID_PASSWORD_HASH,
          Instant.now(),
          Instant.now());
    }
  }

  public static final class Auth {
    /** Opaque string. Just a valid JWT _format_ */
    public static final String VALID_JWT_STRING =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";

    public static final String VALID_REFRESH_RAW_TOKEN_STRING =
        "e4192c47-9649-48be-9f88-523240f45b6e";
    public static final RawToken VALID_REFRESH_RAW_TOKEN =
        ResultAsserts.success(RawToken.of(VALID_REFRESH_RAW_TOKEN_STRING));
    public static final TokenHash VALID_REFRESH_TOKEN_HASH = TokenHash.of(VALID_REFRESH_RAW_TOKEN);

    public static final int MINIMUM_BCRYPT_LOG_ROUNDS = 4;

    public static RefreshToken buildRefreshToken(Instant expiryDate) {
      return RefreshToken.of(VALID_REFRESH_TOKEN_HASH, VALID_USER.id(), expiryDate);
    }
  }
}
