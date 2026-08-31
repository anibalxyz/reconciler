package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for User Domain Entity")
public class UserTest extends UnitTest {
  private static final Instant TIMESTAMP = Instant.now();
  private static final User baseUser = VALID_USER;

  @Test
  @DisplayName("passwordMatches: given correct password, then return true")
  void passwordMatches_correctPassword_returnsTrue() {
    assertThat(VALID_USER.passwordMatches(VALID_PASSWORD_STRING)).isTrue();
  }

  @Test
  @DisplayName("passwordMatches: given wrong password, then return false")
  void passwordMatches_wrongPassword_returnsFalse() {
    assertThat(VALID_USER.passwordMatches("wrong")).isFalse();
  }

  @Test
  @DisplayName("equals: given same reference, then return true")
  void equals_sameReference_returnsTrue() {
    assertThat(baseUser).isEqualTo(baseUser);
  }

  @Test
  @DisplayName("equals: given same id, then return true regardless of other fields")
  void equals_sameId_returnsTrueRegardlessOfOtherFields() {
    Email otherEmail = ResultAsserts.success(Email.of("other" + VALID_USER.email().value()));
    Name otherName = ResultAsserts.success(Name.of("other" + VALID_USER.name().value()));
    Instant later = TIMESTAMP.plusSeconds(60);

    User sameIdDifferentFields =
        User.reconstitute(VALID_USER_ID, otherName, otherEmail, VALID_PASSWORD_HASH, later, later);

    assertThat(baseUser).isEqualTo(sameIdDifferentFields);
  }

  @Test
  @DisplayName("equals: given different id, then return false")
  void equals_differentId_returnsFalse() {
    User otherIdUser = buildUser(baseUser.id().value() + 1);

    assertThat(baseUser).isNotEqualTo(otherIdUser);
  }

  @Test
  @DisplayName("equals: given two transient users with identical fields, then return false")
  void equals_twoTransientUsers_returnsFalseEvenWithIdenticalFields() {
    User transient1 = User.create(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH);
    User transient2 = User.create(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH);

    assertThat(transient1).isNotEqualTo(transient2);
  }

  @Test
  @DisplayName("equals: given a persistent user and a transient user, then return false")
  void equals_persistentVsTransient_returnsFalse() {
    User transientUser = User.create(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH);

    assertThat(baseUser).isNotEqualTo(transientUser);
  }

  @Test
  @DisplayName("equals: given null or a different type, then return false")
  void equals_nullOrDifferentType_returnsFalse() {
    assertThat(baseUser).isNotEqualTo(null);
    assertThat(baseUser).isNotEqualTo("not a user");
  }

  @Test
  @DisplayName("hashCode: given a transient user, then return zero")
  void hashCode_transientUser_returnsZero() {
    User transientUser = User.create(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH);

    assertThat(transientUser.hashCode()).isZero();
  }

  @Test
  @DisplayName("hashCode: given same id, then return same hash code regardless of other fields")
  void hashCode_sameId_returnsSameHashCode() {
    Email otherEmail = ResultAsserts.success(Email.of("other" + baseUser.email().value()));
    User sameIdDifferentFields =
        User.reconstitute(
            VALID_USER_ID, VALID_NAME, otherEmail, VALID_PASSWORD_HASH, TIMESTAMP, TIMESTAMP);

    assertThat(baseUser.hashCode()).isEqualTo(sameIdDifferentFields.hashCode());
  }
}
