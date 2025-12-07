package com.anibalxyz.features.auth.api;

import static com.anibalxyz.features.Constants.Auth.VALID_JWT;
import static com.anibalxyz.features.Constants.Auth.VALID_REFRESH_TOKEN;
import static com.anibalxyz.features.Constants.Environment.*;
import static com.anibalxyz.features.Constants.Users.*;
import static com.anibalxyz.features.Helpers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.RefreshToken;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for AuthController")
public class AuthControllerTest {
  @Mock private AuthService authService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private Context ctx;

  @InjectMocks private AuthController authController;

  @BeforeAll
  public static void setup() {
    Constants.init();
  }

  @BeforeEach
  public void di() {
    authController =
        new AuthController(Constants.APP_CONFIG.env(), authService, refreshTokenService);
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @BeforeEach
    void setUp() {
      when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    @DisplayName("login: given valid credentials, then return JWT")
    public void login_validCredentials_returnJwt() {
      LoginRequest request = new LoginRequest("", "");
      stubBodyValidatorFor(ctx, LoginRequest.class).thenReturn(request);
      RefreshToken dummyRefreshToken = new RefreshToken(1L, "d-token", null, Instant.now(), false);
      AuthResult dummyAuthResult = new AuthResult(VALID_JWT, dummyRefreshToken);
      when(authService.authenticateUser(request)).thenReturn(dummyAuthResult);

      authController.login(ctx);

      verify(ctx).status(200);
      AuthResponse response = capturedJsonAs(ctx, AuthResponse.class);
      assertThat(response.accessToken()).isEqualTo(VALID_JWT);
    }

    @Test
    @DisplayName("logout: given existing refresh token, then clear cookie and revoke token")
    void logout_existingRefreshToken_clearCookieAndRevokeToken() {
      when(ctx.cookie("refreshToken")).thenReturn(VALID_REFRESH_TOKEN);

      authController.logout(ctx);

      verify(refreshTokenService).revokeToken(VALID_REFRESH_TOKEN);
      verify(ctx).status(204);

      Cookie cookie = capturedCookie(ctx);
      assertThat(cookie.getName()).isEqualTo("refreshToken");
      assertThat(cookie.getValue()).isEmpty();
      assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("logout: given no refresh token, then clear cookie")
    void logout_noRefreshToken_clearCookie() {
      when(ctx.cookie("refreshToken")).thenReturn(null);

      authController.logout(ctx);

      verify(refreshTokenService, never()).revokeToken(anyString());
      verify(ctx).status(204);

      Cookie cookie = capturedCookie(ctx);
      assertThat(cookie.getName()).isEqualTo("refreshToken");
      assertThat(cookie.getValue()).isEmpty();
      assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("refresh: given valid refresh token, then return refreshed tokens")
    public void refresh_validRefreshToken_returnRefreshedTokens() {
      Instant expiryDay = Instant.now().plus(6, ChronoUnit.DAYS);
      RefreshToken validRefreshToken =
          new RefreshToken(1L, VALID_REFRESH_TOKEN, VALID_USER, expiryDay, false);
      AuthResult result = new AuthResult(VALID_JWT, validRefreshToken);
      AuthResponse expectedResponse = new AuthResponse(result.accessToken());

      long maxAgeInSeconds =
          Math.max(
              0, validRefreshToken.expiryDate().getEpochSecond() - Instant.now().getEpochSecond());
      Cookie expectedCookie =
          new Cookie(
              "refreshToken",
              validRefreshToken.token(),
              AUTH_COOKIE_PATH, //
              (int) maxAgeInSeconds,
              AUTH_COOKIE_SECURE,
              0,
              true, // HttpOnly
              null, // Comment
              AUTH_COOKIE_DOMAIN, // Domain
              AUTH_COOKIE_SAMESITE);

      when(ctx.cookie("refreshToken")).thenReturn(VALID_REFRESH_TOKEN);
      when(authService.refreshTokens(VALID_REFRESH_TOKEN)).thenReturn(result);

      authController.refresh(ctx);

      Cookie actualCookie = capturedCookie(ctx);
      assertThat(actualCookie).isEqualTo(expectedCookie);

      verify(ctx).status(200);
      AuthResponse actualResponse = capturedJsonAs(ctx, AuthResponse.class);
      assertThat(actualResponse).isEqualTo(expectedResponse);
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("login: given invalid input, then throw ValidationException")
    public void login_invalidInput_throwsValidationException() {
      stubBodyValidatorFor(ctx, LoginRequest.class)
          .thenThrow(
              new ValidationException(
                  Map.of("email", List.of(new ValidationError<>("Email is required")))));

      assertThatThrownBy(() -> authController.login(ctx)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("login: given invalid credentials, then throw InvalidCredentialsException")
    public void login_invalidCredentials_throwsInvalidCredentialsException() {
      LoginRequest request = new LoginRequest("invalid@email.com", "wrongpassword");
      stubBodyValidatorFor(ctx, LoginRequest.class).thenReturn(request);
      when(authService.authenticateUser(request))
          .thenThrow(new InvalidCredentialsException("Invalid credentials"));

      assertThatThrownBy(() -> authController.login(ctx))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("refresh: given missing refresh token cookie, then throw UnauthorizedResponse")
    public void refresh_missingRefreshTokenCookie_throwUnauthorizedResponse() {
      when(ctx.cookie("refreshToken")).thenReturn(null);

      assertThatThrownBy(() -> authController.refresh(ctx))
          .isInstanceOf(UnauthorizedResponse.class)
          .hasMessage("Missing refresh token in cookie");
    }

    @Test
    @DisplayName("refresh: given invalid refresh token, then throw InvalidCredentialsException")
    public void refresh_invalidRefreshToken_throwInvalidCredentialsException() {
      when(ctx.cookie("refreshToken")).thenReturn("invalid-token");
      when(authService.refreshTokens("invalid-token"))
          .thenThrow(new InvalidCredentialsException("Invalid credentials"));

      assertThatThrownBy(() -> authController.refresh(ctx))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid credentials");
    }
  }
}
