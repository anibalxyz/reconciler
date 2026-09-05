package com.anibalxyz.features.auth.api.routes;

import static com.anibalxyz.features.auth.api.AuthCookieService.REFRESH_TOKEN_COOKIE;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN_STRING;
import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.getValueFromCookie;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.auth.api.exception.MissingRefreshTokenCookie;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import java.util.Map;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for POST /refresh")
public class RefreshTokensIT extends AuthIT {

  @Test
  @DisplayName("given missing refreshToken cookie, then respond with 401 MissingRefreshTokenCookie")
  void missingCookie_respond401MissingRefreshTokenCookie() {
    ErrorResult expectedResult = InfrastructureErrorMapper.map(new MissingRefreshTokenCookie());

    Map<String, String> cookie = Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=");

    Response response = http.post("/auth/refresh", "", cookie);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

    String responseCookie = getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
    assertThat(responseCookie).isNullOrEmpty();

    ErrorResponse errorResponse = http.parseBody(response, ErrorResponse.class);
    assertThat(errorResponse.instance()).isNotNull();
    assertThat(errorResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given outside maintenance window, then respond with 503 Unavailable Server")
  void outsideMaintenanceWindow_respond503UnavailableServer() {
    testClock.resetTo(SATURDAY_MIDDAY);
    ErrorResult expectedResult =
        ErrorMapper.map(new AuthenticateUser.Error.MaintenanceWindow(MAINTENANCE_START));

    Map<String, String> cookie =
        Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=" + VALID_REFRESH_RAW_TOKEN_STRING);
    Response response = http.post("/auth/refresh", "", cookie);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(503);

    String responseCookie = getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
    assertThat(responseCookie).isNullOrEmpty();

    ErrorResponse errorResponse = http.parseBody(response, ErrorResponse.class);
    assertThat(errorResponse.instance()).isNotNull();
    assertThat(errorResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given invalid refresh token, then respond with 401 Unauthorized")
  void invalidRefreshToken_respond401Unauthorized() {
    ErrorResult expectedResult =
        ErrorMapper.map(new RefreshTokens.Error.InvalidToken(InvalidRefreshTokenError.notFound()));

    Map<String, String> cookie =
        Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=" + VALID_REFRESH_RAW_TOKEN_STRING);

    Response response = http.post("/auth/refresh", "", cookie);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

    String responseCookie = getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
    assertThat(responseCookie).isNullOrEmpty();

    ErrorResponse errorResponse = http.parseBody(response, ErrorResponse.class);
    assertThat(errorResponse.instance()).isNotNull();
    assertThat(errorResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given valid refresh token, then respond with 200 with refreshed tokens")
  void validRefreshToken_respond200WithRefreshedTokens() {
    User user =
        persistUser(em, VALID_NAME_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING).toDomain();
    LoginResult expectedResult = loginUser(user.email().value(), VALID_PASSWORD_STRING);

    Map<String, String> cookie =
        Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=" + expectedResult.refreshToken);
    Response response = http.post("/auth/refresh", "", cookie);
    assertThat(response.code()).isEqualTo(200);

    AuthResponse authResponse = http.parseBody(response, AuthResponse.class);
    validateJwt(authResponse.accessToken(), user.id().value());

    String refreshTokenCookie =
        getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
    validateRefreshToken(refreshTokenCookie, user.id().value());
  }

  private LoginResult loginUser(String email, String password) {
    LoginRequest loginRequest = new LoginRequest(email, password);
    Response loginResponse = http.post("/auth/login", loginRequest);
    AuthResponse authResponse = http.parseBody(loginResponse, AuthResponse.class);

    return new LoginResult(
        authResponse.accessToken(),
        getValueFromCookie(loginResponse.header("Set-Cookie"), REFRESH_TOKEN_COOKIE));
  }

  private record LoginResult(String accessToken, String refreshToken) {}
}
