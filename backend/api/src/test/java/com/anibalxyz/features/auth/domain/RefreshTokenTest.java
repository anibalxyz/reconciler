package com.anibalxyz.features.auth.domain;

import static com.anibalxyz.shared.Constants.Auth.buildRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest extends UnitTest {

  private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
  private static final Instant PAST = NOW.minusSeconds(60);
  private static final Instant FUTURE = NOW.plusSeconds(60);

  @Test
  @DisplayName("isExpired: given expiry date is in the future, then return false")
  void isExpired_expiryDateInTheFuture_returnFalse() {
    RefreshToken token = buildRefreshToken(FUTURE);
    assertThat(token.isExpired(NOW)).isFalse();
  }

  @Test
  @DisplayName("isExpired: given expiry date is in the past, then return true")
  void isExpired_expiryDateInThePast_returnTrue() {
    RefreshToken token = buildRefreshToken(PAST);
    assertThat(token.isExpired(NOW)).isTrue();
  }

  @Test
  @DisplayName("isExpired: given expiry date equals reference time, then return true")
  void isExpired_expiryDateEqualsNow_returnTrue() {
    RefreshToken token = buildRefreshToken(NOW);
    assertThat(token.isExpired(NOW)).isTrue();
  }

  @Test
  @DisplayName("withRevoked: given revoked is false, then return new instance with revoked true")
  void withRevoked_revokedFalse_returnNewInstanceWithRevokedTrue() {
    RefreshToken original = buildRefreshToken(FUTURE);
    RefreshToken revoked = original.withRevoked(true);

    assertThat(revoked.isRevoked()).isTrue();
    assertThat(original.isRevoked()).isFalse();
  }

  @Test
  @DisplayName("withRevoked: given any revoked value, then preserve all other fields")
  void withRevoked_anyValue_preserveAllOtherFields() {
    RefreshToken original = buildRefreshToken(FUTURE);
    RefreshToken revoked = original.withRevoked(true);

    assertThat(revoked.userId()).isEqualTo(original.userId());
    assertThat(revoked.tokenHash()).isEqualTo(original.tokenHash());
    assertThat(revoked.expiryDate()).isEqualTo(original.expiryDate());
  }

  @Test
  @DisplayName("withRevoked: given revoked is true, then return new instance with revoked false")
  void withRevoked_revokedTrue_returnNewInstanceWithRevokedFalse() {
    RefreshToken original = buildRefreshToken(FUTURE);
    RefreshToken unrevoked = original.withRevoked(false);

    assertThat(unrevoked.isRevoked()).isFalse();
  }
}
