package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.*;
import static com.anibalxyz.shared.MaintenanceTestClock.INSIDE_WINDOW_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.anibalxyz.core.domain.error.ReasonedError;
import com.anibalxyz.features.auth.application.out.AuthResult;
import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("Tests for RefreshTokens use case")
public class RefreshTokensTest extends UnitTest {
  private static final Instant FIXED_INSTANT = Instant.parse("2025-01-01T12:00:00Z");
  private static final Duration DURATION = Duration.ofDays(7);
  private static final EnvStub env = new EnvStub(DURATION);
  private static final Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
  private static final MaintenancePolicy maintenancePolicy = new MaintenancePolicy();

  @Mock private JwtService jwtService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private CreateRefreshToken createRefreshToken;

  private RefreshTokens refreshTokens;

  @BeforeEach
  void deps() {
    refreshTokens =
        new RefreshTokens(
            env, clock, refreshTokenRepository, maintenancePolicy, jwtService, createRefreshToken);
  }

  @Test
  @DisplayName("execute: given outside time window, then return MaintenanceWindow error")
  void execute_outsideWindow_returnMaintenanceWindow() {
    Clock clockOutsideWindow =
        Clock.fixed(INSIDE_WINDOW_TIME.toInstant(), INSIDE_WINDOW_TIME.getZone());
    var serviceOutsideWindow =
        new RefreshTokens(
            env,
            clockOutsideWindow,
            refreshTokenRepository,
            maintenancePolicy,
            jwtService,
            createRefreshToken);

    var result = serviceOutsideWindow.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    verifyNoInteractions(refreshTokenRepository, createRefreshToken, jwtService);
    assertThat(ResultAsserts.failure(result))
        .isInstanceOf(RefreshTokens.Error.MaintenanceWindow.class);
  }

  @Test
  @DisplayName(
      "execute: given invalid raw token, then return InvalidToken with InvalidRefreshTokenError.invalid")
  void execute_invalidRawToken_returnInvalidTokenError() {
    var result = refreshTokens.execute("not-a-valid-uuid");

    var failure = (RefreshTokens.Error.InvalidToken) ResultAsserts.failure(result);
    assertThat(failure.error())
        .isInstanceOf(InvalidRefreshTokenError.class)
        .extracting(ReasonedError::getReason)
        .isInstanceOf(InvalidRefreshTokenError.Reason.Invalid.class);
    verifyNoInteractions(refreshTokenRepository, createRefreshToken, jwtService);
  }

  @Test
  @DisplayName(
      "execute: given valid raw token but not found, then return InvalidToken with InvalidRefreshTokenError.notFound")
  void execute_validRawTokenButNotFound_returnInvalidTokenError() {
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.empty());

    var result = refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    var failure = (RefreshTokens.Error.InvalidToken) ResultAsserts.failure(result);
    assertThat(failure.error())
        .isInstanceOf(InvalidRefreshTokenError.class)
        .extracting(ReasonedError::getReason)
        .isInstanceOf(InvalidRefreshTokenError.Reason.NotFound.class);
    verify(refreshTokenRepository, never()).revoke(any());
    verifyNoInteractions(createRefreshToken, jwtService);
  }

  @Test
  @DisplayName(
      "execute: given valid raw token but expired, then return InvalidToken with InvalidRefreshTokenError.expired")
  void execute_validRawTokenButExpired_returnInvalidTokenError() {
    var expiredToken = buildRefreshToken(clock.instant().minus(1, ChronoUnit.DAYS));
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(expiredToken));

    var result = refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    var failure = (RefreshTokens.Error.InvalidToken) ResultAsserts.failure(result);
    assertThat(failure.error())
        .isInstanceOf(InvalidRefreshTokenError.class)
        .extracting(ReasonedError::getReason)
        .isInstanceOf(InvalidRefreshTokenError.Reason.Expired.class);
    verify(refreshTokenRepository, never()).revoke(any());
    verifyNoInteractions(createRefreshToken, jwtService);
  }

  @Test
  @DisplayName(
      "execute: given valid raw token but revoked, then return InvalidToken with InvalidRefreshTokenError.revoked")
  void execute_validRawTokenButRevoked_returnInvalidTokenError() {
    var revokedToken =
        buildRefreshToken(clock.instant().plus(1, ChronoUnit.DAYS)).withRevoked(true);
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(revokedToken));

    var result = refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    var failure = (RefreshTokens.Error.InvalidToken) ResultAsserts.failure(result);
    assertThat(failure.error())
        .isInstanceOf(InvalidRefreshTokenError.class)
        .extracting(ReasonedError::getReason)
        .isInstanceOf(InvalidRefreshTokenError.Reason.Revoked.class);
    verify(refreshTokenRepository, never()).revoke(any());
    verifyNoInteractions(createRefreshToken, jwtService);
  }

  @Test
  @DisplayName("execute: given valid raw token, then return AuthResult")
  void execute_validRawToken_returnAuthResult() {
    var currentRefreshToken = buildRefreshToken(clock.instant().plus(1, ChronoUnit.DAYS));
    var newRawToken = VALID_REFRESH_RAW_TOKEN;
    var newAccessToken = VALID_JWT_STRING;
    var expiryDate = maintenancePolicy.calculateExpiryDate(ZonedDateTime.now(clock), DURATION);
    var expectedAuthResult = new AuthResult(newAccessToken, newRawToken, expiryDate);

    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(currentRefreshToken));
    when(createRefreshToken.execute(currentRefreshToken.userId(), expiryDate))
        .thenReturn(newRawToken);
    when(jwtService.generateToken(currentRefreshToken.userId().value())).thenReturn(newAccessToken);

    var result = refreshTokens.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    var authResult = ResultAsserts.success(result);
    assertThat(authResult).isEqualTo(expectedAuthResult);
    verify(refreshTokenRepository).revoke(currentRefreshToken.tokenHash());
  }

  private record EnvStub(Duration JWT_REFRESH_EXPIRATION_TIME_DAYS) implements RefreshTokens.Env {}
}
