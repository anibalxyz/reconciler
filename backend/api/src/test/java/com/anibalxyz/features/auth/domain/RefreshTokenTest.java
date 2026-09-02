package com.anibalxyz.features.auth.domain;

import static com.anibalxyz.shared.Constants.Auth.buildRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.anibalxyz.features.users.domain.UserId;
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
  @DisplayName("equals: given same instance, then return true")
  void equals_sameInstance_returnTrue() {
    RefreshToken token = buildRefreshToken(FUTURE);

    assertThat(token.equals(token)).isTrue();
  }

  @Test
  @DisplayName("equals: given null or different class, then return false")
  void equals_nullOrDifferentClass_returnFalse() {
    RefreshToken token = buildRefreshToken(FUTURE);

    assertThat(token.equals(null)).isFalse();
    assertThat(token.equals("some String")).isFalse();
  }

  @Test
  @DisplayName("equals and hashCode: given same tokenHash, then return true and matching hashCode")
  void equalsAndHashCode_sameTokenHash_returnTrueAndMatchingHashCode() {
    TokenHash hash = mock(TokenHash.class);
    UserId userId1 = mock(UserId.class);
    UserId userId2 = mock(UserId.class);

    RefreshToken token1 = RefreshToken.of(hash, userId1, FUTURE);
    RefreshToken token2 = RefreshToken.reconstitute(hash, userId2, PAST, true);

    assertThat(token1).isEqualTo(token2);
    assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
  }

  @Test
  @DisplayName("equals: given different tokenHash, then return false")
  void equals_differentTokenHash_returnFalse() {
    TokenHash hash1 = mock(TokenHash.class);
    TokenHash hash2 = mock(TokenHash.class);
    UserId userId = mock(UserId.class);

    RefreshToken token1 = RefreshToken.of(hash1, userId, FUTURE);
    RefreshToken token2 = RefreshToken.of(hash2, userId, FUTURE);

    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  @DisplayName("equals: given null tokenHash in either instance, then return false")
  void equals_nullTokenHash_returnFalse() {
    TokenHash hash = mock(TokenHash.class);
    UserId userId = mock(UserId.class);

    RefreshToken tokenWithNullHash1 = RefreshToken.of(null, userId, FUTURE);
    RefreshToken tokenWithNullHash2 = RefreshToken.of(null, userId, FUTURE);
    RefreshToken tokenWithHash = RefreshToken.of(hash, userId, FUTURE);

    assertThat(tokenWithNullHash1.equals(tokenWithHash)).isFalse();
    assertThat(tokenWithHash.equals(tokenWithNullHash1)).isFalse();
    assertThat(tokenWithNullHash1.equals(tokenWithNullHash2)).isFalse();
  }
}
