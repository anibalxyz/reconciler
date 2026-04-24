package com.anibalxyz.features.auth.api;

import static com.anibalxyz.shared.Constants.Auth.VALID_JWT;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_TOKEN;
import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.common.application.exception.FailureSignal;
import com.anibalxyz.shared.Constants;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.UnauthorizedResponse;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for AuthController")
public class AuthControllerTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  private static final Clock testClock = Clock.fixed(FIXED_NOW.toInstant(), FIXED_NOW.getZone());

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
        new AuthController(Constants.APP_CONFIG.env(), authService, refreshTokenService, testClock);
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @BeforeEach
    void setUp() {
      when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    @DisplayName("login: given service returns success, then respond 200 with JWT and set cookie")
    public void login_serviceReturnsSuccess_respond200WithJWTAndSetCookie() {
      LoginRequest request = new LoginRequest("", "");
      when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(request);

      RefreshToken dummyRefreshToken =
          new RefreshToken(1L, "d-token", null, FIXED_NOW.toInstant(), false);
      AuthResult dummyAuthResult = new AuthResult(VALID_JWT, dummyRefreshToken);
      when(authService.authenticateUser(request.toCommand()))
          .thenReturn(Result.success(dummyAuthResult));

      authController.login(ctx);

      Cookie cookie = capturedCookie(ctx);
      assertThat(cookie.getName()).isEqualTo("refreshToken");
      assertThat(cookie.getValue()).isEqualTo(dummyAuthResult.refreshToken().token());
      assertThat(cookie.getMaxAge())
          .isEqualTo(dummyAuthResult.refreshToken().secondsUntilExpiry(FIXED_NOW.toInstant()));

      verify(ctx).status(200);
      verify(ctx).json(new AuthResponse(dummyAuthResult.accessToken()));
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
    @DisplayName(
        "refresh: given existing refresh token and service returns success, then respond 200 with refreshed tokens")
    public void
        refresh_existingRefreshTokenAndServiceReturnsSuccess_respond200WithRefreshedTokens() {
      RefreshToken validRefreshToken =
          new RefreshToken(
              1L,
              VALID_REFRESH_TOKEN,
              VALID_USER,
              FIXED_NOW.toInstant().plus(2, ChronoUnit.DAYS),
              false);
      AuthResult result = new AuthResult(VALID_JWT, validRefreshToken);
      AuthResponse expectedResponse = new AuthResponse(result.accessToken());

      when(ctx.cookie("refreshToken")).thenReturn(VALID_REFRESH_TOKEN);
      when(authService.refreshTokens(VALID_REFRESH_TOKEN)).thenReturn(Result.success(result));

      authController.refresh(ctx);

      Cookie actualCookie = capturedCookie(ctx);
      assertThat(actualCookie.getName()).isEqualTo("refreshToken");
      assertThat(actualCookie.getValue()).isEqualTo(result.refreshToken().token());
      assertThat(actualCookie.getMaxAge())
          .isEqualTo(result.refreshToken().secondsUntilExpiry(FIXED_NOW.toInstant()));

      verify(ctx).status(200);
      verify(ctx).json(expectedResponse);
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("login: given service result is failure, then throw FailureSignal with its error")
    public void login_serviceResultIsFailure_throwFailureSignal() {
      LoginRequest request = new LoginRequest("", "");

      when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(request);
      Result<AuthResult, AuthService.AuthenticateUserError> someFailure =
          Result.failure(
              new AuthService.AuthenticateUserError.InvalidCredentials(
                  new InvalidCredentialsError()));
      when(authService.authenticateUser(request.toCommand())).thenReturn(someFailure);

      assertThatThrownBy(() -> authController.login(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(fs -> ((FailureSignal) fs).getError())
          .isInstanceOf(someFailure.getError().getClass());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("refresh: given missing refresh token cookie, then throw UnauthorizedResponse")
    public void refresh_missingRefreshTokenCookie_throwUnauthorizedResponse(String value) {
      when(ctx.cookie("refreshToken")).thenReturn(value);

      assertThatThrownBy(() -> authController.refresh(ctx))
          .isInstanceOf(UnauthorizedResponse.class)
          .hasMessage("Missing refresh token in cookie");
    }

    @Test
    @DisplayName(
        "refresh: given service result is failure, then throw FailureSignal with its error")
    public void refresh_serviceReturnsRefreshTokensError_throwFailureSignal() {
      when(ctx.cookie("refreshToken")).thenReturn(VALID_REFRESH_TOKEN);
      Result<AuthResult, AuthService.RefreshTokensError> someFailure =
          Result.failure(
              new AuthService.RefreshTokensError.InvalidToken(InvalidRefreshTokenError.notFound()));
      when(authService.refreshTokens(VALID_REFRESH_TOKEN)).thenReturn(someFailure);

      assertThatThrownBy(() -> authController.refresh(ctx))
          .isInstanceOf(FailureSignal.class)
          .extracting(fs -> ((FailureSignal) fs).getError())
          .isInstanceOf(someFailure.getError().getClass());
    }
  }
}
