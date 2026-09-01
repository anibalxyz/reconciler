package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.TokenHash;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.users.domain.UserId;
import java.time.*;

public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  /**
   * If the time-window feature is enabled, the expiration date may be capped to the end of the
   * current window.
   */
  public RawToken createRefreshToken(UserId userId, Instant expiryDate) {
    RawToken rawToken = RawToken.generate();
    TokenHash tokenHash = TokenHash.of(rawToken);
    refreshTokenRepository.persist(RefreshToken.of(tokenHash, userId, expiryDate));
    return rawToken;
  }

  public Result<RotationResult, InvalidRefreshTokenError> verifyAndRotate(
      String token, Instant now, Instant expiryDate) {
    return verifyRefreshToken(token, now)
        .onSuccess(oldToken -> refreshTokenRepository.revoke(oldToken.tokenHash()))
        .map(
            oldToken ->
                new RotationResult(
                    oldToken.userId(), createRefreshToken(oldToken.userId(), expiryDate)));
  }

  public Result<RefreshToken, InvalidRefreshTokenError> verifyRefreshToken(
      String rawToken, Instant now) {
    return RawToken.of(rawToken)
        .mapError(invalidRawTokenError -> InvalidRefreshTokenError.invalid())
        .map(TokenHash::of)
        .flatMap(this::findByHash)
        .flatMap(refreshToken -> refreshToken.checkIfExpired(now))
        .flatMap(RefreshToken::checkIfRevoked);
  }

  private Result<RefreshToken, InvalidRefreshTokenError> findByHash(TokenHash tokenHash) {
    return refreshTokenRepository
        .findByTokenHash(tokenHash)
        .<Result<RefreshToken, InvalidRefreshTokenError>>map(Result::success)
        .orElseGet(() -> Result.failure(InvalidRefreshTokenError.notFound()));
  }

  public record RotationResult(UserId userId, RawToken rawToken) {}
}
