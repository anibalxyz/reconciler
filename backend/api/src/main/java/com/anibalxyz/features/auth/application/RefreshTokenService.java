package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.users.domain.User;
import java.time.*;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  /**
   * If the time-window feature is enabled, the expiration date may be capped to the end of the
   * current window.
   */
  public RefreshToken createRefreshToken(User user, Instant expiryDate) {
    return refreshTokenRepository.save(
        new RefreshToken(null, UUID.randomUUID().toString(), user, expiryDate, false));
  }

  public Result<RefreshToken, InvalidRefreshTokenError> verifyAndRotate(
      String token, Instant now, Instant expiryDate) {
    Result<RefreshToken, InvalidRefreshTokenError> verifyResult = verifyRefreshToken(token, now);
    if (verifyResult.isFailure()) {
      return Result.failure(verifyResult.getError());
    }

    RefreshToken oldToken = verifyResult.getValue();
    refreshTokenRepository.save(oldToken.withRevoked(true));
    return Result.success(createRefreshToken(oldToken.user(), expiryDate));
  }

  public Result<RefreshToken, InvalidRefreshTokenError> verifyRefreshToken(
      String token, Instant now) {
    Optional<RefreshToken> found = refreshTokenRepository.findByToken(token);
    if (found.isEmpty()) {
      return Result.failure(InvalidRefreshTokenError.notFound());
    }

    RefreshToken refreshToken = found.get();

    if (refreshToken.isExpired(now)) {
      // NOTE: here could add logic to invalidate all tokens for the user
      // if an expired token is used, as it could signal a token theft attempt.
      return Result.failure(InvalidRefreshTokenError.expired());
    }

    if (refreshToken.revoked()) {
      // NOTE: here could add logic to invalidate all tokens for the user
      // if a revoked token is used, as it could signal a token theft attempt.
      return Result.failure(InvalidRefreshTokenError.revoked());
    }

    return Result.success(refreshToken);
  }

  public void revokeToken(String token) {
    if (token == null || token.isBlank()) return;

    refreshTokenRepository
        .findByToken(token)
        .ifPresent((refreshToken) -> refreshTokenRepository.save(refreshToken.withRevoked(true)));
  }

  public int cleanupExpiredTokens() {
    return refreshTokenRepository.deleteExpiredTokens();
  }
}
