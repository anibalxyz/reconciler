package com.anibalxyz.features.auth.application;

import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.TokenHash;

public class Logout {
  private final RefreshTokenRepository refreshTokenRepository;

  public Logout(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  public void execute(String rawToken) {
    RawToken.of(rawToken).map(TokenHash::of).onSuccess(refreshTokenRepository::revoke);
  }
}
