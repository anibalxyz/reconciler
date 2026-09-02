package com.anibalxyz.features.auth.application;

import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN_STRING;
import static com.anibalxyz.shared.Constants.Auth.buildRefreshToken;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.shared.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for Logout use case")
public class LogoutTest extends UnitTest {
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @InjectMocks private Logout logout;

  @Test
  @DisplayName("execute: given no token, then do nothing")
  void execute_noToken_doNothing() {
    logout.execute(null);
    logout.execute("");

    verify(refreshTokenRepository, never()).persist(any());
    verify(refreshTokenRepository, never()).findByTokenHash(any());
  }

  @Test
  @DisplayName("execute: given token not found, then do nothing")
  void execute_tokenNotFound_doNothing() {
    logout.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    verify(refreshTokenRepository, never()).persist(any());
  }

  @Test
  @DisplayName("execute: given valid token, then revoke it")
  void execute_validToken_revokeIt() {
    RefreshToken refreshToken = buildRefreshToken(Instant.now());

    logout.execute(VALID_REFRESH_RAW_TOKEN_STRING);

    verify(refreshTokenRepository).revoke(refreshToken.tokenHash());
    verify(refreshTokenRepository, never()).persist(any());
  }
}
