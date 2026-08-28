package com.anibalxyz.features.auth.api.routes;

import static com.anibalxyz.features.auth.api.AuthCookieService.REFRESH_TOKEN_COOKIE;
import static com.anibalxyz.features.auth.api.routes.AuthIT.*;
import static com.anibalxyz.shared.Constants.Users.VALID_EMAIL_STRING;
import static com.anibalxyz.shared.Constants.Users.VALID_NAME_STRING;
import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD_STRING;
import static com.anibalxyz.shared.Helpers.getValueFromCookie;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.shared.ResultAsserts;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for POST /login")
public class LoginIT extends AuthIT {
  @Test
  @DisplayName("given validation failed, then respond with 400 validation error")
  void validationFailed_respond400ValidationError() {
    String invalidEmail = "invalid email";
    LoginRequest loginRequest = new LoginRequest(invalidEmail, VALID_PASSWORD_STRING);

    ValidationNotification<UserDomainError> notification = new ValidationNotification<>();
    notification.add("email", ResultAsserts.failure(Email.validate(invalidEmail)));

    ErrorResult expectedResult =
        ErrorMapper.map(new AuthenticateUser.Error.ValidationFailed(notification));

    Response loginResponse = http.post("/auth/login", loginRequest);
    assertThat(loginResponse.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    ErrorResponse authResponse = http.parseBody(loginResponse, ErrorResponse.class);
    assertThat(authResponse.instance()).isNotNull();
    assertThat(authResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given outside maintenance window, respond with 503 Unavailable Server")
  void outsideMaintenanceWindow_respond503UnavailableServer() {
    testClock.resetTo(SATURDAY_MIDDAY);
    LoginRequest loginRequest = new LoginRequest(VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    ErrorResult expectedResult =
        ErrorMapper.map(new AuthenticateUser.Error.MaintenanceWindow(MAINTENANCE_START));

    Response loginResponse = http.post("/auth/login", loginRequest);
    assertThat(loginResponse.code()).isEqualTo(expectedResult.status()).isEqualTo(503);

    ErrorResponse authResponse = http.parseBody(loginResponse, ErrorResponse.class);
    assertThat(authResponse.instance()).isNotNull();
    assertThat(authResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given invalid credentials, respond with 401 Auth")
  void invalidCredentials_respond401Unauthorized() {
    User user =
        persistUser(em, VALID_NAME_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING).toDomain();
    LoginRequest loginRequest =
        new LoginRequest("different" + user.email().value(), VALID_PASSWORD_STRING);
    ErrorResult expectedResult =
        ErrorMapper.map(
            new AuthenticateUser.Error.InvalidCredentials(new InvalidCredentialsError()));

    Response loginResponse = http.post("/auth/login", loginRequest);
    assertThat(loginResponse.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

    ErrorResponse authResponse = http.parseBody(loginResponse, ErrorResponse.class);
    assertThat(authResponse.instance()).isNotNull();
    assertThat(authResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given valid credentials, respond 200 with refresh and access tokens")
  void validCredentials_respond200WithTokens() {
    User user =
        persistUser(em, VALID_NAME_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING).toDomain();
    LoginRequest loginRequest = new LoginRequest(user.email().value(), VALID_PASSWORD_STRING);

    Response loginResponse = http.post("/auth/login", loginRequest);
    assertThat(loginResponse.code()).isEqualTo(200);

    AuthResponse authResponse = http.parseBody(loginResponse, AuthResponse.class);
    validateJwt(authResponse.accessToken(), user.id().value());

    String refreshTokenCookie =
        getValueFromCookie(loginResponse.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
    validateRefreshToken(refreshTokenCookie, user.id().value());
  }
}
