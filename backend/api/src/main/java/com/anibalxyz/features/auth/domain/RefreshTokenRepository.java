package com.anibalxyz.features.auth.domain;

import java.util.Optional;

public interface RefreshTokenRepository {
  Optional<RefreshToken> findByTokenHash(TokenHash tokenHash);

  void persist(RefreshToken refreshToken);

  void revoke(TokenHash tokenHash);

  int deleteExpiredTokens();
}
