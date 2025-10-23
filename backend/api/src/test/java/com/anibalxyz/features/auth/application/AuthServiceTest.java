package com.anibalxyz.features.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.application.in.LoginPayload;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
public class AuthServiceTest {
  private static final String VALID_PASSWORD = "V4L|D_Passw0Rd";
  private static final String VALID_EMAIL = "valid@email.com";
  private static final int SALT_ROUNDS = 8;
  private static final String VALID_JWT = "some.valid.jwt";

  private AuthService authService;
  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

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
    authService = new AuthService(userService, jwtService, refreshTokenService);
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @Test
    @DisplayName("authenticateUser: given valid credentials, then return AuthResult")
    public void authenticateUser_validCredentials_returnAuthResult() {
      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);
      Instant now = Instant.now();
      User user =
          new User(
              1,
              "Name",
              new Email(payload.email()),
              PasswordHash.generate(payload.password(), SALT_ROUNDS),
              now,
              now);
      when(userService.getUserByEmail(VALID_EMAIL)).thenReturn(user);
      when(jwtService.generateToken(anyInt())).thenReturn(VALID_JWT);
      when(refreshTokenService.createRefreshToken(any(User.class)))
          .thenReturn(new RefreshToken(1L, "dummy-token", user, Instant.now().plusSeconds(1000), false));

      assertDoesNotThrow(
          () -> {
            AuthResult authResult = authService.authenticateUser(payload);
            assertNotNull(authResult.accessToken());
            assertNotNull(authResult.refreshToken());
            assertEquals(VALID_JWT, authResult.accessToken());
          });
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("authenticateUser: given invalid password, then throw InvalidCredentialsException")
    public void authenticateUser_invalidPassword_throwAuthenticationException() {
      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);
      Instant now = Instant.now();

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
    @DisplayName("authenticateUser: given invalid email, then throw InvalidCredentialsException")
    public void authenticateUser_invalidEmail_throwAuthenticationException() {
      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);

      when(userService.getUserByEmail(payload.email())).thenThrow(ResourceNotFoundException.class);

      assertThatThrownBy(() -> authService.authenticateUser(payload))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid credentials");
    }
  }
}
