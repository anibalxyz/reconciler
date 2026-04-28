package com.anibalxyz.features.auth.domain;

import com.anibalxyz.features.users.domain.User;
import java.time.Instant;

public record RefreshToken(Long id, String token, User user, Instant expiryDate, boolean revoked) {
  // TODO: token should be correctly typed
  // TODO: implement factory method
  // TODO: should save UserId instead entire User

  public boolean isExpired(Instant now) {
    return secondsUntilExpiry(now) <= 0;
  }

  public long secondsUntilExpiry(Instant now) {
    return Math.max(0, expiryDate.getEpochSecond() - now.getEpochSecond());
  }

  public RefreshToken withRevoked(boolean revoked) {
    return new RefreshToken(id, token, user, expiryDate, revoked);
  }
}
