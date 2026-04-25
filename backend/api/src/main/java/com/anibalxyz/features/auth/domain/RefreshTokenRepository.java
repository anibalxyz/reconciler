package com.anibalxyz.features.auth.domain;

import java.util.Optional;

public interface RefreshTokenRepository {
  Optional<RefreshToken> findByToken(String token);

  RefreshToken save(RefreshToken refreshToken);

  int deleteExpiredTokens();
}
