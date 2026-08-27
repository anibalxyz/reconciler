package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.*;
import static com.anibalxyz.shared.Constants.Auth.VALID_JWT_STRING;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("Tests for RefreshTokens use case")
public class RefreshTokensTest extends UnitTest {
  private static final Instant FIXED_INSTANT = Instant.parse("2025-01-01T12:00:00Z");
  private static final ZoneId ZONE = ZoneId.of("America/Montevideo");
  private static final ZonedDateTime SATURDAY_MORNING =
      LocalDateTime.of(2025, 12, 6, 8, 0).atZone(ZONE);
  private static final Duration DURATION = Duration.ofDays(7);
  private static final EnvStub env = new EnvStub(DURATION);
  private static final Clock clock = Clock.fixed(FIXED_INSTANT, ZONE);
  private static final MaintenancePolicy maintenancePolicy = new MaintenancePolicy();

  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  private RefreshTokens refreshTokens;

  @BeforeEach
  void deps() {
    refreshTokens =
        new RefreshTokens(env, clock, maintenancePolicy, jwtService, refreshTokenService);
  }

  @Test
  @DisplayName("given valid command but outside time window, then return MaintenanceWindow error")
  void validCommandOutsideWindow_returnMaintenanceWindow() {
    Clock clockOutsideWindow =
        Clock.fixed(SATURDAY_MORNING.toInstant(), SATURDAY_MORNING.getZone());
    var serviceOutsideWindow =
        new RefreshTokens(
            env, clockOutsideWindow, maintenancePolicy, jwtService, refreshTokenService);

    var result = serviceOutsideWindow.execute(VALID_REFRESH_RAW_TOKEN_STRING);
    assertThat(ResultAsserts.failure(result))
        .isInstanceOf(RefreshTokens.Error.MaintenanceWindow.class);
  }

  @Test
  @DisplayName("given refresh token rotation failed, then return InvalidToken error")
  void refreshTokenRotationFailed_returnInvalidToken() {
    var expiryDate = maintenancePolicy.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
    var error = InvalidRefreshTokenError.notFound();
    when(refreshTokenService.verifyAndRotate(
            VALID_REFRESH_RAW_TOKEN_STRING, clock.instant(), expiryDate))
        .thenReturn(Result.failure(error));

    var result = refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING);
    var failure = ResultAsserts.failure(result);

    var invalidTokenClass = RefreshTokens.Error.InvalidToken.class;
    assertThat(failure)
        .isInstanceOf(invalidTokenClass)
        .extracting(e -> (invalidTokenClass.cast(e).error()))
        .isEqualTo(error);
  }

  @Test
  @DisplayName("given refresh token rotation failed, then return InvalidToken error")
  void validRefreshToken_returnAuthResult() {
    var expiryDate = maintenancePolicy.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
    RefreshToken currentRefreshToken = buildRefreshToken(clock.instant().plus(1, ChronoUnit.DAYS));

    when(refreshTokenService.verifyAndRotate(
            VALID_REFRESH_RAW_TOKEN_STRING, clock.instant(), expiryDate))
        .thenReturn(
            Result.success(
                new RefreshTokenService.RotationResult(
                    currentRefreshToken.userId(), VALID_REFRESH_RAW_TOKEN)));
    when(jwtService.generateToken(currentRefreshToken.userId().value()))
        .thenReturn(VALID_JWT_STRING);

    var result = refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING);
    AuthResult authResult = ResultAsserts.success(result);
    assertThat(authResult.accessToken()).isEqualTo(VALID_JWT_STRING);
    assertThat(authResult.refreshToken()).isEqualTo(VALID_REFRESH_RAW_TOKEN);
  }

  private record EnvStub(Duration JWT_REFRESH_EXPIRATION_TIME_DAYS) implements RefreshTokens.Env {}
}
