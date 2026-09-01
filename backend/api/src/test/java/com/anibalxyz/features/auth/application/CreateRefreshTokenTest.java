package com.anibalxyz.features.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.anibalxyz.features.auth.domain.RawToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.domain.TokenHash;
import com.anibalxyz.features.users.domain.UserId;
import com.anibalxyz.shared.UnitTest;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for CreateRefreshToken use case")
class CreateRefreshTokenTest extends UnitTest {

  private static final Instant EXPIRY_DATE = Instant.parse("2026-12-31T23:59:59Z");

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private CreateRefreshToken createRefreshToken;

  @Test
  @DisplayName(
      "execute: given valid userId and expiryDate, then persist RefreshToken and return RawToken")
  void execute_validInputs_persistsRefreshTokenAndReturnsRawToken() {
    UserId userId = mock(UserId.class);

    RawToken rawToken = createRefreshToken.execute(userId, EXPIRY_DATE);

    assertThat(rawToken).isNotNull();
    assertThat(rawToken.value()).isNotBlank();

    verify(refreshTokenRepository)
        .persist(
            argThat(
                token ->
                    Objects.equals(token.userId(), userId)
                        && Objects.equals(token.expiryDate(), EXPIRY_DATE)
                        && Objects.equals(token.tokenHash(), TokenHash.of(rawToken))
                        && !token.isRevoked()));
  }
}
