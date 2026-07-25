package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_TOKEN;
import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for RefreshTokenService")
class RefreshTokenServiceTest {
  private static final Duration EXPIRATION = Duration.ofDays(7);
  private static final Instant FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).toInstant(ZoneOffset.UTC);
  @Mock private RefreshTokenRepository refreshTokenRepository;
  private RefreshTokenService refreshTokenService;

  @BeforeAll
  static void init() {
    Constants.init();
  }

  @BeforeEach
  void di() {
    refreshTokenService = new RefreshTokenService(refreshTokenRepository);
  }

  private RefreshToken buildToken(Instant expiryDate, boolean revoked) {
    return new RefreshToken(1L, VALID_REFRESH_TOKEN, VALID_USER, expiryDate, revoked);
  }

  private RefreshToken persistToken(long id, RefreshToken t) {
    return new RefreshToken(id, t.token(), t.user(), t.expiryDate(), t.revoked());
  }

  private RefreshToken persistToken(RefreshToken t) {
    return persistToken(1L, t);
  }

  @Test
  @DisplayName("createRefreshToken: given a User, then persist and return refresh token")
  void createRefreshToken_user_persistAndReturnRefreshToken() {
    Instant expiryDate = FIXED_NOW.plus(EXPIRATION);
    RefreshToken expected = buildToken(expiryDate, false);
    when(refreshTokenRepository.save(
            argThat(
                refreshToken ->
                    refreshToken.id() == null
                        // This could be a UUID check if it were a dependency instead static call
                        && refreshToken.user().equals(VALID_USER)
                        && refreshToken.expiryDate().equals(expiryDate)
                        && !refreshToken.revoked())))
        .thenAnswer(i -> persistToken(i.getArgument(0)));

    RefreshToken actual = refreshTokenService.createRefreshToken(VALID_USER, expiryDate);

    assertThat(actual.id()).isNotNull();
    assertThat(actual).usingRecursiveComparison().ignoringFields("id", "token").isEqualTo(expected);
  }

  @Test
  @DisplayName("verifyRefreshToken: given valid RefreshToken, then return success")
  void verifyRefreshToken_validRefreshToken_returnSuccess() {
    RefreshToken token = buildToken(FIXED_NOW.plus(1, ChronoUnit.DAYS), false);
    when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.of(token));

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_TOKEN, FIXED_NOW);

    RefreshToken value = ResultAsserts.success(result);
    assertThat(value).isEqualTo(token);
  }

  @Test
  @DisplayName("verifyRefreshToken: given token not found, then return failure with NotFound")
  void verifyRefreshToken_tokenNotFound_returnFailureWithNotFound() {
    when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.empty());

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_TOKEN, FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.NotFound.class);
  }

  @Test
  @DisplayName("verifyRefreshToken: given expired token, then return failure with Expired reason")
  void verifyRefreshToken_expiredToken_returnFailureWithExpired() {
    RefreshToken token = buildToken(FIXED_NOW.minusSeconds(60), false);
    when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.of(token));

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_TOKEN, FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.Expired.class);
  }

  @Test
  @DisplayName("verifyRefreshToken: given revoked token, then return failure with Revoked reason")
  void verifyRefreshToken_revokedToken_returnFailureWithRevoked() {
    RefreshToken token = buildToken(FIXED_NOW.plus(1, ChronoUnit.DAYS), true);
    when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.of(token));

    var result = refreshTokenService.verifyRefreshToken(VALID_REFRESH_TOKEN, FIXED_NOW);

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.Revoked.class);
  }

  @Test
  @DisplayName("verifyAndRotate: given verification failed, then return verification error")
  void verifyAndRotate_verificationFailed_returnVerificationError() {
    // simple way to stub verifyRefreshToken() internals to return an error
    when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.empty());

    Instant now = FIXED_NOW;
    var result = refreshTokenService.verifyAndRotate(VALID_REFRESH_TOKEN, now, now.plusSeconds(10));

    assertThat(ResultAsserts.failure(result).getReason())
        .isInstanceOf(InvalidRefreshTokenError.Reason.NotFound.class);
  }

  @Test
  @DisplayName(
      "verifyAndRotate: given verification passed, then revoke the old token and return the new one")
  void verifyAndRotate_validationPassed_revokeOldAndReturnNew() {
    Instant now = FIXED_NOW;
    Instant oldExpiryDate = now.plus(10, ChronoUnit.MINUTES); // still valid
    Instant newExpiryDate = oldExpiryDate.plus(3, ChronoUnit.DAYS);

    RefreshToken refreshToken = buildToken(oldExpiryDate, false);

    when(refreshTokenRepository.findByToken(refreshToken.token()))
        .thenReturn(Optional.of(refreshToken));
    Function<RefreshToken, Boolean> isRevokedToken =
        r ->
            Objects.equals(r.id(), refreshToken.id())
                && Objects.equals(r.token(), refreshToken.token())
                && Objects.equals(r.user(), refreshToken.user())
                && Objects.equals(r.expiryDate(), refreshToken.expiryDate())
                && r.revoked();
    Function<RefreshToken, Boolean> wasCorrectlyUpdated =
        r ->
            r.id() == null
                && Objects.equals(r.user(), refreshToken.user())
                && Objects.equals(r.expiryDate(), newExpiryDate)
                && !r.revoked();

    long newId = refreshToken.id() + 1;
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(
            i -> {
              RefreshToken arg = i.getArgument(0);
              // 1) revokeToken() internal call
              if (isRevokedToken.apply(arg)) {
                return arg;
              }
              // 2) createRefreshToken() internal call
              if (wasCorrectlyUpdated.apply(arg)) {
                return persistToken(newId, arg);
              }

              return fail("Method called with an unexpected argument: ", arg);
            });

    var result = refreshTokenService.verifyAndRotate(refreshToken.token(), now, newExpiryDate);

    verify(refreshTokenRepository).save(argThat(isRevokedToken::apply));
    verify(refreshTokenRepository).save(argThat(wasCorrectlyUpdated::apply));

    RefreshToken newRefreshToken = ResultAsserts.success(result);

    assertThat(newRefreshToken.id()).isEqualTo(newId);
    assertThat(newRefreshToken.token()).isNotEqualTo(refreshToken.token());
    assertThat(newRefreshToken.user()).isEqualTo(refreshToken.user());
    assertThat(newRefreshToken.expiryDate()).isEqualTo(newExpiryDate);
  }

  @Test
  @DisplayName("revokeToken: given no token, then do nothing")
  void revokeToken_noToken_doNothing() {
    refreshTokenService.revokeToken(null);
    refreshTokenService.revokeToken("");

    verify(refreshTokenRepository, never()).save(any());
    verify(refreshTokenRepository, never()).findByToken(any());
  }

  @Test
  @DisplayName("revokeToken: given token not found, then do nothing")
  void revokeToken_tokenNotFound_doNothing() {
    when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.empty());

    refreshTokenService.revokeToken(VALID_REFRESH_TOKEN);

    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("revokeToken: given valid token, then revoke it")
  void revokeToken_validToken_revokeIt() {
    RefreshToken refreshToken = buildToken(FIXED_NOW, false);
    when(refreshTokenRepository.findByToken(refreshToken.token()))
        .thenReturn(Optional.of(refreshToken));

    refreshTokenService.revokeToken(VALID_REFRESH_TOKEN);

    verify(refreshTokenRepository)
        .save(
            argThat(
                r ->
                    Objects.equals(r.id(), refreshToken.id())
                        && Objects.equals(r.token(), refreshToken.token())
                        && Objects.equals(r.user(), refreshToken.user())
                        && Objects.equals(r.expiryDate(), refreshToken.expiryDate())
                        && r.revoked()));
  }

  @Test
  @DisplayName("cleanupExpiredTokens: once called, then return count of deleted tokens")
  void cleanupExpiredTokens_returnCount() {
    int expectedCount = 213;
    when(refreshTokenRepository.deleteExpiredTokens()).thenReturn(expectedCount);

    int actualCount = refreshTokenService.cleanupExpiredTokens();

    assertThat(actualCount).isEqualTo(expectedCount);
  }
}
