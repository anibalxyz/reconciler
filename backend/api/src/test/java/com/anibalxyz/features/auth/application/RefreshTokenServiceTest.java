package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for RefreshTokenService")
class RefreshTokenServiceTest extends UnitTest {
  private static final Instant FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).toInstant(ZoneOffset.UTC);
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @InjectMocks private RefreshTokenService refreshTokenService;

  @Test
  @DisplayName(
      "verifyRefreshToken: given invalid raw token string, then return failure with Invalid reason")
  void verifyRefreshToken_invalidRawToken_returnFailureWithInvalid() {
    var result = refreshTokenService.verifyRefreshToken("invalid-raw-token", FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.Invalid.class);
  }

  @Test
  @DisplayName("verifyRefreshToken: given token not found, then return failure with NotFound")
  void verifyRefreshToken_tokenNotFound_returnFailureWithNotFound() {
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.empty());

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_RAW_TOKEN_STRING, FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.NotFound.class);
  }

  @Test
  @DisplayName("verifyRefreshToken: given expired token, then return failure with Expired reason")
  void verifyRefreshToken_expiredToken_returnFailureWithExpired() {
    RefreshToken token = buildRefreshToken(FIXED_NOW.minusSeconds(60));
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_RAW_TOKEN_STRING, FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.Expired.class);
  }

  @Test
  @DisplayName("verifyRefreshToken: given revoked token, then return failure with Revoked reason")
  void verifyRefreshToken_revokedToken_returnFailureWithRevoked() {
    RefreshToken token = buildRefreshToken(FIXED_NOW.plus(1, ChronoUnit.DAYS)).withRevoked(true);
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_RAW_TOKEN_STRING, FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.Revoked.class);
  }

  @Test
  @DisplayName("verifyAndRotate: given verification failed, then return verification error")
  void verifyAndRotate_verificationFailed_returnVerificationError() {
    // simple way to stub verifyRefreshToken() internals to return an error
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.empty());

    Instant now = FIXED_NOW;
    var result =
        refreshTokenService.verifyAndRotate(
            VALID_REFRESH_RAW_TOKEN_STRING, now, now.plusSeconds(10));

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.NotFound.class);
  }

  @Test
  @DisplayName("verifyAndRotate: given verification passed, then return RotationResult")
  void verifyAndRotate_verificationPassed_returnRotationResult() {
    Instant newExpiryDate = FIXED_NOW.plus(3, ChronoUnit.DAYS);
    RefreshToken oldToken = buildRefreshToken(FIXED_NOW.plus(1, ChronoUnit.DAYS));
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(oldToken));

    var result =
        refreshTokenService.verifyAndRotate(
            VALID_REFRESH_RAW_TOKEN_STRING, FIXED_NOW, newExpiryDate);

    verify(refreshTokenRepository).revoke(oldToken.tokenHash());
    verify(refreshTokenRepository)
        .persist(
            argThat(
                r ->
                    Objects.equals(r.userId(), oldToken.userId())
                        && Objects.equals(r.expiryDate(), newExpiryDate)
                        && !r.isRevoked()
                        && !Objects.equals(r.tokenHash(), oldToken.tokenHash())));

    RefreshTokenService.RotationResult rotation = ResultAsserts.success(result);
    assertThat(rotation.userId()).isEqualTo(oldToken.userId());
    assertThat(rotation.rawToken().value()).isNotEqualTo(VALID_REFRESH_RAW_TOKEN_STRING);
    assertThat(RawToken.isValid(rotation.rawToken().value())).isTrue();
  }

  @Test
  @DisplayName("verifyRefreshToken: given valid RefreshToken, then return success")
  void verifyRefreshToken_validRefreshToken_returnSuccess() {
    RefreshToken token = buildRefreshToken(FIXED_NOW.plus(1, ChronoUnit.DAYS));
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_RAW_TOKEN_STRING, FIXED_NOW);

    RefreshToken value = ResultAsserts.success(result);
    assertThat(value).isEqualTo(token);
  }

  @Test
  @DisplayName("revokeToken: given no token, then do nothing")
  void revokeToken_noToken_doNothing() {
    refreshTokenService.revokeToken(null);
    refreshTokenService.revokeToken("");

    verify(refreshTokenRepository, never()).persist(any());
    verify(refreshTokenRepository, never()).findByTokenHash(any());
  }

  @Test
  @DisplayName("revokeToken: given token not found, then do nothing")
  void revokeToken_tokenNotFound_doNothing() {
    when(refreshTokenRepository.findByTokenHash(VALID_REFRESH_TOKEN_HASH))
        .thenReturn(Optional.empty());

    refreshTokenService.revokeToken(VALID_REFRESH_RAW_TOKEN_STRING);

    verify(refreshTokenRepository, never()).persist(any());
  }

  @Test
  @DisplayName("revokeToken: given valid token, then revoke it")
  void revokeToken_validToken_revokeIt() {
    RefreshToken refreshToken = buildRefreshToken(FIXED_NOW);
    when(refreshTokenRepository.findByTokenHash(refreshToken.tokenHash()))
        .thenReturn(Optional.of(refreshToken));

    refreshTokenService.revokeToken(VALID_REFRESH_RAW_TOKEN_STRING);

    verify(refreshTokenRepository).revoke(refreshToken.tokenHash());
    verify(refreshTokenRepository, never()).persist(any());
  }

  @Test
  @DisplayName("cleanupExpiredTokens: given method is called, then return count of deleted tokens")
  void cleanupExpiredTokens_methodCalled_returnCount() {
    int expectedCount = 213;
    when(refreshTokenRepository.deleteExpiredTokens()).thenReturn(expectedCount);

    int actualCount = refreshTokenService.cleanupExpiredTokens();

    assertThat(actualCount).isEqualTo(expectedCount);
  }
}
