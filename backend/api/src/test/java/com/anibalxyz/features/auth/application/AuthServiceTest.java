package com.anibalxyz.features.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.application.in.LoginPayload;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.config.environment.ConfigurationFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
  private static final String VALID_PASSWORD = "V4L|D_Passw0Rd";
  private static final String VALID_EMAIL = "valid@email.com";
  private static final int SALT_ROUNDS = 8;

  private static AuthEnvironment env;
  private AuthService authService;
  @Mock private UserService userService;

  @BeforeAll
  public static void setup() {
    env = ConfigurationFactory.loadForTest().env();
  }

  private static LoginPayload createPayload(String email, String password) {
    return new LoginPayload() {
      @Override
      public String email() {
        return email;
      }

      @Override
      public String password() {
        return password;
      }
    };
  }

  @BeforeEach
  public void dependencyInjection() {
    authService = new AuthService(env, userService);
  }

  @Nested
  class SuccessScenarios {

    @Test
    public void authenticateUser_validCredentials_returnJwt() {
      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);
      LocalDateTime now = LocalDateTime.now();
      when(userService.getUserByEmail(VALID_EMAIL))
          .thenReturn(
              new User(
                  1,
                  "Name",
                  new Email(payload.email()),
                  PasswordHash.generate(payload.password(), SALT_ROUNDS),
                  now,
                  now));
      assertDoesNotThrow(
          () -> {
            String jwt = authService.authenticateUser(payload);
            assertTrue(jwt != null && !jwt.isEmpty());
          });
    }
  }

  @Nested
  class FailureScenarios {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"too_short"})
    public void constructor_jwtSecretIsInvalid_throwIllegalArgumentException(String jwtSecret) {
      assertThatThrownBy(
              () -> {
                AuthEnvironment badEnv =
                    new AuthEnvironment() {
                      @Override
                      public String JWT_SECRET() {
                        return jwtSecret;
                      }

                      @Override
                      public String JWT_ISSUER() {
                        return env.JWT_ISSUER();
                      }

                      @Override
                      public Duration JWT_EXPIRATION_TIME() {
                        return env.JWT_EXPIRATION_TIME();
                      }
                    };
                new AuthService(badEnv, userService);
              })
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageStartingWith("JWT_SECRET must");
    }

    @Test
    public void authenticateUser_invalidPassword_throwAuthenticationException() {
      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);
      LocalDateTime now = LocalDateTime.now();

      when(userService.getUserByEmail(VALID_EMAIL))
          .thenReturn(
              new User(
                  1,
                  "Name",
                  new Email(payload.email()),
                  PasswordHash.generate(payload.password() + "makeItDifferent", SALT_ROUNDS),
                  now,
                  now));

      assertThatThrownBy(() -> authService.authenticateUser(payload))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid credentials");
    }

    @Test
    public void authenticateUser_invalidEmail_throwAuthenticationException() {
      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);

      when(userService.getUserByEmail(payload.email())).thenThrow(ResourceNotFoundException.class);

      assertThatThrownBy(() -> authService.authenticateUser(payload))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid credentials");
    }
  }
}
