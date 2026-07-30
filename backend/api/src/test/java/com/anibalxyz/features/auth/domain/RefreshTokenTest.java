package com.anibalxyz.features.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.shared.ResultAsserts;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  private static final int BCRYPT_LOG_ROUNDS = 4;
  private static final String VALID_PASSWORD = "validPassword123";
  // This is the correct way of testing time-based code
  // TODO: look for clock-dependant code tests and use this approach
  private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
  private static final Instant PAST = NOW.minusSeconds(60);
  private static final Instant FUTURE = NOW.plusSeconds(60);

  private static User buildUser() {
    return User.reconstitute(
        1,
        ResultAsserts.success(Name.of("User")),
        ResultAsserts.success(Email.of("user@example.com")),
        ResultAsserts.success(PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS)),
        Instant.now(),
        Instant.now());
  }

  // TODO: may be refactored to Helpers
  private static RefreshToken buildToken(Instant expiryDate) {
    return new RefreshToken(1L, "token-value", buildUser(), expiryDate, false);
  }

  @Test
  @DisplayName("isExpired: given expiry date is in the future, then return false")
  void isExpired_expiryDateInTheFuture_returnFalse() {
    RefreshToken token = buildToken(FUTURE);
    assertThat(token.isExpired(NOW)).isFalse();
  }

  @Test
  @DisplayName("isExpired: given expiry date is in the past, then return true")
  void isExpired_expiryDateInThePast_returnTrue() {
    RefreshToken token = buildToken(PAST);
    assertThat(token.isExpired(NOW)).isTrue();
  }

  @Test
  @DisplayName("isExpired: given expiry date equals reference time, then return true")
  void isExpired_expiryDateEqualsNow_returnTrue() {
    RefreshToken token = buildToken(NOW);
    assertThat(token.isExpired(NOW)).isTrue();
  }

  @Test
  @DisplayName("secondsUntilExpiry: calculations")
  void secondsUntilExpiry_calculations() {
    // Case 1: Future (1 hour later)
    RefreshToken futureToken = buildToken(NOW.plusSeconds(3600));
    assertThat(futureToken.secondsUntilExpiry(NOW)).isEqualTo(3600);

    // Case 2: Exact present (expired exactly now)
    RefreshToken presentToken = buildToken(NOW);
    assertThat(presentToken.secondsUntilExpiry(NOW)).isZero();

    // Case 3: Past (expired 10 seconds ago)
    RefreshToken pastToken = buildToken(NOW.minusSeconds(10));
    assertThat(pastToken.secondsUntilExpiry(NOW)).isZero();
  }

  @Test
  @DisplayName("withRevoked: given revoked is false, then return new instance with revoked true")
  void withRevoked_revokedFalse_returnNewInstanceWithRevokedTrue() {
    RefreshToken original = buildToken(FUTURE);
    RefreshToken revoked = original.withRevoked(true);

    assertThat(revoked.revoked()).isTrue();
    assertThat(original.revoked()).isFalse();
  }

  @Test
  @DisplayName("withRevoked: given any revoked value, then preserve all other fields")
  void withRevoked_anyValue_preserveAllOtherFields() {
    RefreshToken original = buildToken(FUTURE);
    RefreshToken revoked = original.withRevoked(true);

    assertThat(revoked.id()).isEqualTo(original.id());
    assertThat(revoked.token()).isEqualTo(original.token());
    assertThat(revoked.user()).isEqualTo(original.user());
    assertThat(revoked.expiryDate()).isEqualTo(original.expiryDate());
  }

  @Test
  @DisplayName("withRevoked: given revoked is true, then return new instance with revoked false")
  void withRevoked_revokedTrue_returnNewInstanceWithRevokedFalse() {
    RefreshToken original = new RefreshToken(1L, "token-value", buildUser(), FUTURE, true);
    RefreshToken unrevoked = original.withRevoked(false);

    assertThat(unrevoked.revoked()).isFalse();
  }
}
