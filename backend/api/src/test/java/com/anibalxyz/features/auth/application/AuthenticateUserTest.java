package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.VALID_JWT_STRING;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN;
import static com.anibalxyz.shared.Constants.Users.VALID_EMAIL_STRING;
import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD_STRING;
import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.users.application.GetUserByEmail;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

@DisplayName("Tests for AuthenticateUsers use case")
public class AuthenticateUserTest extends UnitTest {
  private static final Instant FIXED_INSTANT = Instant.parse("2025-01-01T12:00:00Z");
  private static final ZoneId ZONE = ZoneId.of("America/Montevideo");
  private static final ZonedDateTime SATURDAY_MORNING =
      LocalDateTime.of(2025, 12, 6, 8, 0).atZone(ZONE);
  private static final Duration DURATION = Duration.ofDays(7);
  private static final EnvStub env = new EnvStub(DURATION);
  private static final Clock clock = Clock.fixed(FIXED_INSTANT, ZONE);
  private static final MaintenancePolicy maintenancePolicy = new MaintenancePolicy();

  @Mock private GetUserByEmail getUserByEmail;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  private AuthenticateUser authenticateUser;

  @BeforeEach
  void deps() {
    authenticateUser =
        new AuthenticateUser(
            env, clock, maintenancePolicy, getUserByEmail, jwtService, refreshTokenService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"password", "email"})
  @DisplayName("given there is a field error, then return ValidationFailed error")
  void fieldError_returnValidationFailed(String field) {
    String email = field.equals("email") ? " " : VALID_EMAIL_STRING;
    String password = field.equals("password") ? " " : VALID_PASSWORD_STRING;
    LoginCommand command = new LoginCommand(email, password);

    var result = authenticateUser.execute(command);

    var failure = ResultAsserts.failure(result);

    var validationFailedClass = AuthenticateUser.Error.ValidationFailed.class;
    assertThat(failure).isInstanceOf(validationFailedClass);

    var errors = validationFailedClass.cast(failure).notification().getErrors();
    assertThat(errors.size()).isEqualTo(1);
    assertThat(errors.getFirst().field()).isEqualTo(field);
  }

  @Test
  @DisplayName("given there are field errors, then return ValidationFailed error")
  void fieldErrors_returnValidationFailed() {
    LoginCommand command = new LoginCommand(" ", " ");

    var result = authenticateUser.execute(command);

    var failure = ResultAsserts.failure(result);

    var validationFailedClass = AuthenticateUser.Error.ValidationFailed.class;
    assertThat(failure).isInstanceOf(validationFailedClass);

    var errors = validationFailedClass.cast(failure).notification().getErrors();
    assertThat(errors.size()).isEqualTo(2);
    assertThat(errors)
        .extracting(ValidationNotification.ErrorEntry::field)
        .containsExactlyInAnyOrder("email", "password");
  }

  @Test
  @DisplayName("given valid command but outside time window, then return MaintenanceWindow error")
  void validCommandOutsideWindow_returnMaintenanceWindow() {
    LoginCommand command = new LoginCommand(VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    Clock clockOutsideWindow =
        Clock.fixed(SATURDAY_MORNING.toInstant(), SATURDAY_MORNING.getZone());
    var authenticateUserOutsideWindow =
        new AuthenticateUser(
            env,
            clockOutsideWindow,
            maintenancePolicy,
            getUserByEmail,
            jwtService,
            refreshTokenService);

    var result = authenticateUserOutsideWindow.execute(command);
    assertThat(ResultAsserts.failure(result))
        .isInstanceOf(AuthenticateUser.Error.MaintenanceWindow.class);
  }

  @Test
  @DisplayName("given user not found, then return InvalidCredentials error")
  void userByEmailNotFound_returnInvalidCredentials() {
    LoginCommand command = new LoginCommand(VALID_EMAIL_STRING, VALID_PASSWORD_STRING);
    var expectedError =
        new AuthenticateUser.Error.InvalidCredentials(new InvalidCredentialsError());

    when(getUserByEmail.execute(command.email()))
        .thenReturn(Result.failure(UserNotFoundError.byEmail(VALID_EMAIL_STRING)));

    var result = authenticateUser.execute(command);
    assertThat(ResultAsserts.failure(result)).isEqualTo(expectedError);
  }

  @Test
  @DisplayName("given password is incorrect, then return InvalidCredentials error")
  void passwordIsIncorrect_returnInvalidCredentials() {
    User user = VALID_USER;
    String differentPassword = VALID_PASSWORD_STRING + "makeItDifferent";
    LoginCommand command = new LoginCommand(user.email().value(), differentPassword);
    var expectedError =
        new AuthenticateUser.Error.InvalidCredentials(new InvalidCredentialsError());

    when(getUserByEmail.execute(command.email())).thenReturn(Result.success(user));

    var result = authenticateUser.execute(command);
    assertThat(ResultAsserts.failure(result)).isEqualTo(expectedError);
  }

  @Test
  @DisplayName("given valid command, then return AuthResult")
  void validCommand_returnAuthResult() {
    User user = VALID_USER;
    LoginCommand command = new LoginCommand(VALID_EMAIL_STRING, VALID_PASSWORD_STRING);
    Instant expiryDate = maintenancePolicy.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
    AuthResult expectedResult =
        new AuthResult(VALID_JWT_STRING, VALID_REFRESH_RAW_TOKEN, expiryDate);

    when(getUserByEmail.execute(command.email())).thenReturn(Result.success(user));
    when(jwtService.generateToken(user.id().value())).thenReturn(expectedResult.accessToken());
    when(refreshTokenService.createRefreshToken(user.id(), expiryDate))
        .thenReturn(expectedResult.refreshToken());

    var result = authenticateUser.execute(command);
    AuthResult authResult = ResultAsserts.success(result);
    assertThat(authResult).isEqualTo(expectedResult);
  }

  private record EnvStub(Duration JWT_REFRESH_EXPIRATION_TIME_DAYS)
      implements AuthenticateUser.Env {}
}
