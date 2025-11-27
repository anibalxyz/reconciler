package com.anibalxyz.features.auth.application;

import static com.anibalxyz.features.Constants.Auth.VALID_JWT;
import static com.anibalxyz.features.Constants.Auth.VALID_REFRESH_TOKEN;
import static com.anibalxyz.features.Constants.Environment.BCRYPT_LOG_ROUNDS;
import static com.anibalxyz.features.Constants.Users.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.application.in.LoginPayload;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for AuthService")
public class AuthServiceTest {
  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthService authService;

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

  @BeforeAll
  public static void setup() {
    Constants.init();
  }

  @BeforeEach
  public void di() {
    authService =
        new AuthService(userService, jwtService, refreshTokenService, Constants.APP_CONFIG.env());
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
              PasswordHash.generate(payload.password(), BCRYPT_LOG_ROUNDS),
              now,
              now);
      when(userService.getUserByEmail(VALID_EMAIL)).thenReturn(user);
      when(jwtService.generateToken(anyInt())).thenReturn(VALID_JWT);
      when(refreshTokenService.createRefreshToken(any(User.class)))
          .thenReturn(
              new RefreshToken(1L, "dummy-token", user, Instant.now().plusSeconds(1000), false));

      assertDoesNotThrow(
          () -> {
            AuthResult authResult = authService.authenticateUser(payload);
            assertNotNull(authResult.accessToken());
            assertNotNull(authResult.refreshToken());
            assertEquals(VALID_JWT, authResult.accessToken());
          });
    }

    @Test
    @DisplayName("refreshTokens: given valid refresh token string, then return AuthResult")
    public void refreshTokens_validRefreshTokenString_returnAuthResult() {
      RefreshToken newRefreshToken =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(1000), false);
      AuthResult expectedResult = new AuthResult(VALID_JWT, newRefreshToken);

      when(refreshTokenService.verifyAndRotate(VALID_REFRESH_TOKEN)).thenReturn(newRefreshToken);
      when(jwtService.generateToken(VALID_USER.getId())).thenReturn(VALID_JWT);

      AuthResult actualResult = authService.refreshTokens(VALID_REFRESH_TOKEN);

      assertEquals(expectedResult, actualResult);
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
                  PasswordHash.generate(payload.password() + "makeItDifferent", BCRYPT_LOG_ROUNDS),
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

    @Test
    @DisplayName(
        "refreshTokes: given invalid or missing refresh token string, then throw InvalidCredentialsException")
    public void refreshTokes_invalidOrMissingRefreshTokenString_throwInvalidCredentialsException() {
      when(refreshTokenService.verifyAndRotate(null)).thenThrow(InvalidCredentialsException.class);
      assertThatThrownBy(() -> authService.refreshTokens(null))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }
}
