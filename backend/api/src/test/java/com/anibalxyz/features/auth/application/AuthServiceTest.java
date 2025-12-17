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
import com.anibalxyz.features.auth.application.env.AuthEnvironment;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.application.in.LoginPayload;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for AuthService")
public class AuthServiceTest {
  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private AuthEnvironment authEnvironment;
  @Mock private Supplier<ZonedDateTime> clock;

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
  public static void init() {
    Constants.init();
  }

  private ZonedDateTime next(DayOfWeek day, int hour, int minute) {
    return ZonedDateTime.now(ZoneId.of("America/Montevideo"))
        .with(TemporalAdjusters.nextOrSame(day))
        .with(LocalTime.of(hour, minute));
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {
    static Stream<Arguments> authSuccessScenarios() {
      return Stream.of(
          Arguments.of(
              false, DayOfWeek.THURSDAY, 20, 45), // disabled window -> time does not matter
          Arguments.of(true, DayOfWeek.THURSDAY, 20, 45), // enabled window -> normal day
          Arguments.of(true, DayOfWeek.FRIDAY, 19, 59), // edge case -> just before window ending
          Arguments.of(true, DayOfWeek.MONDAY, 8, 0) // edge case -> just after window starting
          );
    }

    @ParameterizedTest
    @MethodSource("authSuccessScenarios")
    @DisplayName("authenticateUser: given valid credentials, then return AuthResult")
    public void authenticateUser_validCredentials_returnAuthResult(
        boolean useWindow, DayOfWeek day, int hour, int minute) {

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

      when(authEnvironment.AUTH_ENABLE_TIME_WINDOW()).thenReturn(useWindow);
      if (useWindow) when(clock.get()).thenReturn(next(day, hour, minute));
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

    @ParameterizedTest
    @MethodSource("authSuccessScenarios")
    @DisplayName("refreshTokens: given valid refresh token string, then return AuthResult")
    public void refreshTokens_validRefreshTokenString_returnAuthResult(
        boolean useWindow, DayOfWeek day, int hour, int minute) {

      RefreshToken newRefreshToken =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(1000), false);
      AuthResult expectedResult = new AuthResult(VALID_JWT, newRefreshToken);

      when(authEnvironment.AUTH_ENABLE_TIME_WINDOW()).thenReturn(useWindow);
      if (useWindow) when(clock.get()).thenReturn(next(day, hour, minute));
      when(refreshTokenService.verifyAndRotate(VALID_REFRESH_TOKEN)).thenReturn(newRefreshToken);
      when(jwtService.generateToken(VALID_USER.getId())).thenReturn(VALID_JWT);

      AuthResult actualResult = authService.refreshTokens(VALID_REFRESH_TOKEN);

      assertEquals(expectedResult, actualResult);
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    static Stream<Arguments> blockedTimeScenarios() {
      return Stream.of(
          Arguments.of(DayOfWeek.FRIDAY, 20, 1), // just after window starting
          Arguments.of(DayOfWeek.FRIDAY, 23, 59),
          Arguments.of(DayOfWeek.SATURDAY, 0, 0),
          Arguments.of(DayOfWeek.SATURDAY, 12, 0),
          Arguments.of(DayOfWeek.SUNDAY, 0, 0),
          Arguments.of(DayOfWeek.SUNDAY, 23, 59),
          Arguments.of(DayOfWeek.MONDAY, 0, 0),
          Arguments.of(DayOfWeek.MONDAY, 7, 59) // just before window ending
          );
    }

    @BeforeEach
    public void setup() {
      when(authEnvironment.AUTH_ENABLE_TIME_WINDOW()).thenReturn(true);
      // by default, assume it is within a valid time window, so can test specific cases separately
      when(clock.get()).thenReturn(next(DayOfWeek.THURSDAY, 20, 45));
    }

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

    @ParameterizedTest
    @MethodSource("blockedTimeScenarios")
    @DisplayName("authenticateUser: outside time window, then throw InvalidCredentialsException")
    public void authenticateUser_outsideWindow_throwInvalidCredentialsException(
        DayOfWeek day, int hour, int minute) {
      when(authEnvironment.AUTH_ENABLE_TIME_WINDOW()).thenReturn(true);
      when(clock.get()).thenReturn(next(day, hour, minute));

      LoginPayload payload = createPayload(VALID_EMAIL, VALID_PASSWORD);

      assertThatThrownBy(() -> authService.authenticateUser(payload))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Refresh is disabled during maintenance window");
    }
  }
}
