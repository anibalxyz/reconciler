package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.VALID_JWT_STRING;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_TOKEN_STRING;
import static com.anibalxyz.shared.Constants.Users.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.auth.application.env.AuthEnvironment;
import com.anibalxyz.features.auth.application.in.LoginCommand;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.users.application.GetUserByEmail;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for AuthService")
class AuthServiceTest extends UnitTest {
  private static final Instant FIXED_INSTANT = Instant.parse("2025-01-01T12:00:00Z");
  private static final ZoneId ZONE = ZoneId.of("America/Montevideo");
  private static final ZonedDateTime SATURDAY_MORNING =
      LocalDateTime.of(2025, 12, 6, 8, 0).atZone(ZONE);
  private static final Duration DURATION = Duration.ofDays(7);
  private static final AuthEnvironmentStub env = new AuthEnvironmentStub(DURATION);
  private static final Clock clock = Clock.fixed(FIXED_INSTANT, ZONE);

  @Mock private GetUserByEmail getUserByEmail;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;
  private AuthService authService;

  @BeforeEach
  void deps() {
    authService = new AuthService(env, clock, getUserByEmail, jwtService, refreshTokenService);
  }

  private RefreshToken buildRefreshToken(Instant expiryDate) {
    return new RefreshToken(1L, VALID_REFRESH_TOKEN_STRING, VALID_USER, expiryDate, false);
  }

  private record AuthEnvironmentStub(Duration JWT_REFRESH_EXPIRATION_TIME_DAYS)
      implements AuthEnvironment {}

  @Nested
  @DisplayName("Tests for Expiry Policy logic")
  class TokenExpiryPolicy {
    @Test
    @DisplayName("calculateExpiryDate: given expiry before Friday 20:00, then return normal expiry")
    void calculateExpiryDate_expiryBeforeFriday_returnNormal() {
      // Monday, April 20, 10:00 AM
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZONE);
      Duration exp = Duration.ofDays(2);

      Instant result = AuthService.calculateExpiryDate(now, exp);

      assertThat(result).isEqualTo(now.plus(exp).toInstant());
    }

    @Test
    @DisplayName("calculateExpiryDate: given expiry after Friday 20:00, then cap at Friday 20:00")
    void calculateExpiryDate_expiryAfterFriday_capAtFriday() {
      // Monday, April 20, 10:00 AM
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZONE);
      Duration exp = Duration.ofDays(8); // Would expire next week (after next Friday)

      Instant expected = ZonedDateTime.of(2026, 4, 24, 20, 0, 0, 0, ZONE).toInstant();

      Instant result = AuthService.calculateExpiryDate(now, exp);

      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName(
        "calculateExpiryDate: given now is Friday after 20:00, then return Friday 20:00 (past)")
    void calculateExpiryDate_nowIsLateFriday_returnPastLimit() {
      // Friday, April 24, 20:00 (The deadline has passed)
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 24, 22, 0, 0, 0, ZONE);
      Duration exp = Duration.ofHours(5);

      // nextOrSame(FRIDAY) returns TODAY. By setting it to 20:00, the result is the past.
      Instant expected = ZonedDateTime.of(2026, 4, 24, 20, 0, 0, 0, ZONE).toInstant();

      Instant result = AuthService.calculateExpiryDate(now, exp);

      assertThat(result).isEqualTo(expected);
      assertThat(result).isBefore(now.toInstant());
    }

    @Test
    @DisplayName(
        "calculateExpiryDate: given now is exactly Friday 20:00, then return that same instant")
    void calculateExpiryDate_exactlyAtLimit_returnSameInstant() {
      ZonedDateTime limit = ZonedDateTime.of(2026, 4, 24, 20, 0, 0, 0, ZONE);
      Duration exp = Duration.ofDays(1);

      Instant result = AuthService.calculateExpiryDate(limit, exp);

      assertThat(result).isEqualTo(limit.toInstant());
    }
  }

  @Nested
  @DisplayName("Tests for System Access Policy logic")
  class SystemAccessPolicy {

    @Test
    @DisplayName("blockedUntil: given now is a valid weekday, then return empty")
    void blockedUntil_weekday_returnEmpty() {
      // Tuesday 10:00
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZONE);

      assertThat(AuthService.blockedUntil(now)).isEmpty();
    }

    @Test
    @DisplayName("blockedUntil: exactly Friday 20:00 should still be open (empty)")
    void blockedUntil_exactlyFriday20pm_returnEmpty() {
      // Friday 20:00
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 24, 20, 0, 0, 0, ZONE);
      assertThat(AuthService.blockedUntil(now)).isEmpty();
    }

    @Test
    @DisplayName("blockedUntil: given now is Friday 20:01, then return next Monday 08:00")
    void blockedUntil_fridayAfter20pm_returnNextMonday() {
      // Friday 20:01
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 24, 20, 1, 0, 0, ZONE);
      Instant expected = ZonedDateTime.of(2026, 4, 27, 8, 0, 0, 0, ZONE).toInstant();

      assertThat(AuthService.blockedUntil(now)).contains(expected);
    }

    @Test
    @DisplayName("blockedUntil: given now is a Sunday, then return next Monday 08:00")
    void blockedUntil_sunday_returnNextMonday() {
      // Sunday 15:00
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 26, 15, 0, 0, 0, ZONE);
      Instant expected = ZonedDateTime.of(2026, 4, 27, 8, 0, 0, 0, ZONE).toInstant();

      assertThat(AuthService.blockedUntil(now)).contains(expected);
    }

    @Test
    @DisplayName("blockedUntil: given now is a Saturday, then return next Monday 08:00")
    void blockedUntil_saturday_returnNextMonday() {
      // Saturday 15:00
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 25, 15, 0, 0, 0, ZONE);
      Instant expected = ZonedDateTime.of(2026, 4, 27, 8, 0, 0, 0, ZONE).toInstant();

      assertThat(AuthService.blockedUntil(now)).contains(expected);
    }

    @Test
    @DisplayName("blockedUntil: given now is Monday before 08:00, then return next Monday 08:00")
    void blockedUntil_mondayBefore8am_returnNextMonday() {
      // Monday 07:59
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 27, 7, 59, 0, 0, ZONE);
      Instant expected = ZonedDateTime.of(2026, 4, 27, 8, 0, 0, 0, ZONE).toInstant();

      assertThat(AuthService.blockedUntil(now)).contains(expected);
    }

    @Test
    @DisplayName("blockedUntil: exactly Monday 08:00 should be open (empty)")
    void blockedUntil_exactlyMonday08am_returnEmpty() {
      // Monday 08:00
      ZonedDateTime now = ZonedDateTime.of(2026, 4, 27, 8, 0, 0, 0, ZONE);
      assertThat(AuthService.blockedUntil(now)).isEmpty();
    }
  }

  @Nested
  @DisplayName("Tests for authenticateUsers()")
  class authenticateUser {
    @ParameterizedTest
    @ValueSource(strings = {"password", "email"})
    @DisplayName("given there is a field error, then return ValidationFailed error")
    void fieldError_returnValidationFailed(String field) {
      String email = field.equals("email") ? " " : VALID_EMAIL_STRING;
      String password = field.equals("password") ? " " : VALID_PASSWORD_STRING;
      LoginCommand command = new LoginCommand(email, password);

      var result = authService.authenticateUser(command);

      var failure = ResultAsserts.failure(result);

      var validationFailedClass = AuthService.AuthenticateUserError.ValidationFailed.class;
      assertThat(failure).isInstanceOf(validationFailedClass);

      var errors = validationFailedClass.cast(failure).notification().getErrors();
      assertThat(errors.size()).isEqualTo(1);
      assertThat(errors.getFirst().field()).isEqualTo(field);
    }

    @Test
    @DisplayName("given there are field errors, then return ValidationFailed error")
    void fieldErrors_returnValidationFailed() {
      LoginCommand command = new LoginCommand(" ", " ");

      var result = authService.authenticateUser(command);

      var failure = ResultAsserts.failure(result);

      var validationFailedClass = AuthService.AuthenticateUserError.ValidationFailed.class;
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
      var serviceOutsideWindow =
          new AuthService(env, clockOutsideWindow, getUserByEmail, jwtService, refreshTokenService);

      var result = serviceOutsideWindow.authenticateUser(command);
      assertThat(ResultAsserts.failure(result))
          .isInstanceOf(AuthService.AuthenticateUserError.MaintenanceWindow.class);
    }

    @Test
    @DisplayName("given user not found, then return InvalidCredentials error")
    void userByEmailNotFound_returnInvalidCredentials() {
      LoginCommand command = new LoginCommand(VALID_EMAIL_STRING, VALID_PASSWORD_STRING);
      var expectedError =
          new AuthService.AuthenticateUserError.InvalidCredentials(new InvalidCredentialsError());

      when(getUserByEmail.execute(command.email()))
          .thenReturn(Result.failure(UserNotFoundError.byEmail(VALID_EMAIL_STRING)));

      var result = authService.authenticateUser(command);
      assertThat(ResultAsserts.failure(result)).isEqualTo(expectedError);
    }

    @Test
    @DisplayName("given password is incorrect, then return InvalidCredentials error")
    void passwordIsIncorrect_returnInvalidCredentials() {
      User user = VALID_USER;
      String differentPassword = VALID_PASSWORD_STRING + "makeItDifferent";
      LoginCommand command = new LoginCommand(user.email().value(), differentPassword);
      var expectedError =
          new AuthService.AuthenticateUserError.InvalidCredentials(new InvalidCredentialsError());

      when(getUserByEmail.execute(command.email())).thenReturn(Result.success(user));

      var result = authService.authenticateUser(command);
      assertThat(ResultAsserts.failure(result)).isEqualTo(expectedError);
    }

    @Test
    @DisplayName("given valid command, then return AuthResult")
    void validCommand_returnAuthResult() {
      User user = VALID_USER;
      LoginCommand command = new LoginCommand(VALID_EMAIL_STRING, VALID_PASSWORD_STRING);
      Instant expiryDate = AuthService.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
      RefreshToken refreshToken = buildRefreshToken(expiryDate);
      AuthResult expectedResult = new AuthResult(VALID_JWT_STRING, refreshToken);

      when(getUserByEmail.execute(command.email())).thenReturn(Result.success(user));
      when(jwtService.generateToken(user.id().value())).thenReturn(expectedResult.accessToken());
      when(refreshTokenService.createRefreshToken(user, expiryDate))
          .thenReturn(expectedResult.refreshToken());

      var result = authService.authenticateUser(command);
      AuthResult authResult = ResultAsserts.success(result);
      assertThat(authResult).isEqualTo(expectedResult);
    }
  }

  @Nested
  @DisplayName("Tests for refreshTokens()")
  class refreshTokens {

    @Test
    @DisplayName("given valid command but outside time window, then return MaintenanceWindow error")
    void validCommandOutsideWindow_returnMaintenanceWindow() {
      Clock clockOutsideWindow =
          Clock.fixed(SATURDAY_MORNING.toInstant(), SATURDAY_MORNING.getZone());
      var serviceOutsideWindow =
          new AuthService(env, clockOutsideWindow, getUserByEmail, jwtService, refreshTokenService);

      var result = serviceOutsideWindow.refreshTokens(VALID_REFRESH_TOKEN_STRING);
      assertThat(ResultAsserts.failure(result))
          .isInstanceOf(AuthService.RefreshTokensError.MaintenanceWindow.class);
    }

    @Test
    @DisplayName("given refresh token rotation failed, then return InvalidToken error")
    void refreshTokenRotationFailed_returnInvalidToken() {
      var expiryDate = AuthService.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
      var error = InvalidRefreshTokenError.notFound();
      when(refreshTokenService.verifyAndRotate(
              VALID_REFRESH_TOKEN_STRING, clock.instant(), expiryDate))
          .thenReturn(Result.failure(error));

      var result = authService.refreshTokens(VALID_REFRESH_TOKEN_STRING);
      var failure = ResultAsserts.failure(result);

      var invalidTokenClass = AuthService.RefreshTokensError.InvalidToken.class;
      assertThat(failure)
          .isInstanceOf(invalidTokenClass)
          .extracting(e -> (invalidTokenClass.cast(e).error()))
          .isEqualTo(error);
    }

    @Test
    @DisplayName("given refresh token rotation failed, then return InvalidToken error")
    void validRefreshToken_returnAuthResult() {
      var expiryDate = AuthService.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
      RefreshToken currentRefreshToken =
          buildRefreshToken(clock.instant().plus(1, ChronoUnit.DAYS));
      RefreshToken expectedRefreshToken = buildRefreshToken(expiryDate);

      when(refreshTokenService.verifyAndRotate(
              currentRefreshToken.token(), clock.instant(), expiryDate))
          .thenReturn(Result.success(expectedRefreshToken));
      when(jwtService.generateToken(expectedRefreshToken.user().id().value()))
          .thenReturn(VALID_JWT_STRING);

      var result = authService.refreshTokens(currentRefreshToken.token());
      AuthResult authResult = ResultAsserts.success(result);
      assertThat(authResult.accessToken()).isEqualTo(VALID_JWT_STRING);
      assertThat(authResult.refreshToken()).isEqualTo(expectedRefreshToken);
    }
  }
}
