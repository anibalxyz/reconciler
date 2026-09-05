package com.anibalxyz.features.auth.api;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.auth.api.exception.MissingOrInvalidAuthHeader;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.IntegrationTest;
import java.time.*;
import java.util.Map;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// NOTE: `ANY_endpoint` tests refer to any *protected* endpoint.
//       With the current implementation (Apr 13/2026), that means it has a required role != GUEST
@DisplayName("Tests for JwtMiddleware")
public class JwtMiddlewareIT extends IntegrationTest {

  private String loginUser(String email) {
    LoginRequest loginRequest = new LoginRequest(email, VALID_PASSWORD_STRING);
    Response loginResponse = http.post("/auth/login", loginRequest);
    AuthResponse authResponse = http.parseBody(loginResponse, AuthResponse.class);
    return authResponse.accessToken();
  }

  @ParameterizedTest
  @ValueSource(strings = {"missingHeader", "invalidHeader", "missingJwt"})
  @DisplayName("ANY /*: given missing JWT, then respond with 401 MissingOrInvalidAuthHeader")
  void ANY_endpoint_missingJwt_respond401MissingOrInvalidAuthHeader(String cause) {
    ErrorResult expectedResult = InfrastructureErrorMapper.map(new MissingOrInvalidAuthHeader());

    Map<String, String> headers =
        switch (cause) {
          case "missingHeader" -> Map.of();
          case "invalidHeader" ->
              Map.of(JwtMiddleware.AUTHORIZATION_HEADER, "invalid" + JwtMiddleware.BEARER_PREFIX);
          case "missingJwt" ->
              Map.of(JwtMiddleware.AUTHORIZATION_HEADER, JwtMiddleware.BEARER_PREFIX);
          default -> throw new IllegalStateException("Unexpected value: " + cause);
        };

    Response response = http.get("/users/", headers);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

    ErrorResponse actualResponseBody = http.parseBody(response, ErrorResponse.class);
    assertThat(actualResponseBody.instance()).isNotNull();
    assertThat(actualResponseBody.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("GET /users: given invalid JWT, then respond 401 Auth")
  void GET_users_invalidJwt_respond401Unauthorized() {
    ErrorResult expectedResult = ErrorMapper.map(new JwtService.JwtValidationError.Invalid());

    Map<String, String> headers = createJwtHeader("invalid-token");
    Response response = http.get("/users/", headers);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

    ErrorResponse actualResponseBody = http.parseBody(response, ErrorResponse.class);
    assertThat(actualResponseBody.instance()).isNotNull();
    assertThat(actualResponseBody.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("GET /users: given expired JWT, then respond 401 Auth")
  void GET_users_expiredJwt_respond401Unauthorized() {
    User user =
        persistUser(em, VALID_NAME_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING).toDomain();
    long jwtAccessExpirationTimeMinutes = Constants.APP_ENV.JWT_ACCESS_EXPIRATION_TIME_SECONDS();
    long justExpiredTime = jwtAccessExpirationTimeMinutes + 60;
    Clock clockInThePast = Clock.offset(testClock, Duration.ofSeconds(-justExpiredTime));
    JwtService jwtService = new JwtService(Constants.APP_CONFIG.env(), clockInThePast);
    String expiredJwt = jwtService.generateToken(user.id().value());

    ErrorResult expectedResult = ErrorMapper.map(new JwtService.JwtValidationError.Expired());

    Map<String, String> headers = createJwtHeader(expiredJwt);
    Response response = http.get("/users/", headers);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

    ErrorResponse actualResponseBody = http.parseBody(response, ErrorResponse.class);
    assertThat(actualResponseBody.instance()).isNotNull();
    assertThat(actualResponseBody.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("ANY /*: given valid JWT, then authorize user")
  void ANY_endpoint_validJwt_authorizeUser() {
    User user =
        persistUser(em, VALID_NAME_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING).toDomain();
    String validJwt = loginUser(user.email().value());

    Map<String, String> headers = createJwtHeader(validJwt);
    Response response = http.get("/users", headers);
    assertThat(response.code()).isEqualTo(200);
  }
}
