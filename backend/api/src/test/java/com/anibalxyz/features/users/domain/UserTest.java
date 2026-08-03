package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for User Domain Object")
public class UserTest {
  private static final int ID = 1;
  private static final Instant TIMESTAMP = Instant.now();

  private static User baseUser;

  @BeforeAll
  public static void setup() {
    Constants.init();
    baseUser = VALID_USER;
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
        User.reconstitute(ID, otherName, otherEmail, VALID_PASSWORD, later, later);

    assertThat(baseUser).isEqualTo(sameIdDifferentFields);
  }

  @Test
  @DisplayName("equals: given different id, then return false")
  void equals_differentId_returnsFalse() {
    User otherIdUser = buildUser(baseUser.id() + 1);

    assertThat(baseUser).isNotEqualTo(otherIdUser);
  }

  @Test
  @DisplayName("equals: given two transient users with identical fields, then return false")
  void equals_twoTransientUsers_returnsFalseEvenWithIdenticalFields() {
    User transient1 = User.create(VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
    User transient2 = User.create(VALID_NAME, VALID_EMAIL, VALID_PASSWORD);

    assertThat(transient1).isNotEqualTo(transient2);
  }

  @Test
  @DisplayName("equals: given null or a different type, then return false")
  void equals_nullOrDifferentType_returnsFalse() {
    assertThat(baseUser).isNotEqualTo(null);
    assertThat(baseUser).isNotEqualTo("not a user");
  }

  @Test
  @DisplayName("hashCode: given same id, then return same hash code regardless of other fields")
  void hashCode_sameId_returnsSameHashCode() {
    Email otherEmail = ResultAsserts.success(Email.of("other" + baseUser.email().value()));
    User sameIdDifferentFields =
        User.reconstitute(ID, VALID_NAME, otherEmail, VALID_PASSWORD, TIMESTAMP, TIMESTAMP);

    assertThat(baseUser.hashCode()).isEqualTo(sameIdDifferentFields.hashCode());
  }
}
