package com.anibalxyz.features.auth.domain;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.users.domain.UserId;
import java.time.Instant;
import java.util.Objects;

public final class RefreshToken {

  private final TokenHash tokenHash;
  private final UserId userId;
  private final Instant expiryDate;
  private final boolean revoked;

  private RefreshToken(TokenHash tokenHash, UserId userId, Instant expiryDate, boolean revoked) {
    this.tokenHash = tokenHash;
    this.userId = userId;
    this.expiryDate = expiryDate;
    this.revoked = revoked;
  }

  public static RefreshToken of(TokenHash tokenHash, UserId userId, Instant expiryDate) {
    return new RefreshToken(tokenHash, userId, expiryDate, false);
  }

  public static RefreshToken reconstitute(
      TokenHash tokenHash, UserId userId, Instant expiryDate, boolean revoked) {
    return new RefreshToken(tokenHash, userId, expiryDate, revoked);
  }

  public boolean isExpired(Instant now) {
    return !now.isBefore(expiryDate);
  }

  public RefreshToken withRevoked(boolean revoked) {
    return new RefreshToken(tokenHash, userId, expiryDate, revoked);
  }

  public UserId userId() {
    return userId;
  }

  public TokenHash tokenHash() {
    return tokenHash;
  }

  public Instant expiryDate() {
    return expiryDate;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public Result<RefreshToken, InvalidRefreshTokenError> checkIfExpired(Instant now) {
    if (isExpired(now)) {
      // NOTE: here could add logic to invalidate all tokens for the user
      // if an expired token is used, as it could signal a token theft attempt.
      return Result.failure(InvalidRefreshTokenError.expired());
    }
    return Result.success(this);
  }

  public Result<RefreshToken, InvalidRefreshTokenError> checkIfRevoked() {
    if (isRevoked()) {
      // NOTE: here could add logic to invalidate all tokens for the user
      // if a revoked token is used, as it could signal a token theft attempt.
      return Result.failure(InvalidRefreshTokenError.revoked());
    }

    return Result.success(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(tokenHash);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RefreshToken other)) return false;
    if (this.tokenHash == null || other.tokenHash == null) return false;
    return Objects.equals(tokenHash, other.tokenHash);
  }
}
