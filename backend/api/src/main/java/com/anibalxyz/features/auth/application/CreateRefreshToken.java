package com.anibalxyz.features.auth.application;

import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.TokenHash;
import com.anibalxyz.features.users.domain.UserId;
import java.time.Instant;

public class CreateRefreshToken {

  private final RefreshTokenRepository refreshTokenRepository;

  public CreateRefreshToken(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  /**
   * If the time-window feature is enabled, the expiration date may be capped to the end of the
   * current window.
   */
  public RawToken execute(UserId userId, Instant expiryDate) {
    RawToken rawToken = RawToken.generate();
    TokenHash tokenHash = TokenHash.of(rawToken);
    refreshTokenRepository.persist(RefreshToken.of(tokenHash, userId, expiryDate));
    return rawToken;
  }
}
