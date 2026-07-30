package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.VALID_EMAIL;
import static com.anibalxyz.shared.Constants.Users.VALID_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for User Domain Object")
public class UserTest {
  private static final int ID = 1;
  private static final Instant TIMESTAMP = Instant.now();
  public static int BCRYPT_LOG_ROUNDS;
  private static Email EMAIL;
  private static Name NAME;
  private static PasswordHash PASSWORD_HASH;

  private User baseUser;

  @BeforeAll
  public static void setup() {
    Constants.init();
    BCRYPT_LOG_ROUNDS = Constants.APP_ENV.BCRYPT_LOG_ROUNDS();
    EMAIL = ResultAsserts.success(Email.of(VALID_EMAIL));
    NAME = ResultAsserts.success(Name.of(VALID_NAME));
    PASSWORD_HASH = ResultAsserts.success(PasswordHash.generate("password1234", BCRYPT_LOG_ROUNDS));
  }

  @BeforeEach
  void setUp() {
    baseUser = User.reconstitute(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
  }

  @Test
  @DisplayName("equals: given same reference, then return true")
  void equals_sameReference_returnsTrue() {
    assertThat(baseUser).isEqualTo(baseUser);
  }

  @Test
  @DisplayName("equals: given same id, then return true regardless of other fields")
  void equals_sameId_returnsTrueRegardlessOfOtherFields() {
    Email otherEmail = ResultAsserts.success(Email.of("other@mail.com"));
    Name otherName = ResultAsserts.success(Name.of("Other Name"));
    Instant later = TIMESTAMP.plusSeconds(60);

    User sameIdDifferentFields =
        User.reconstitute(ID, otherName, otherEmail, PASSWORD_HASH, later, later);

    assertThat(baseUser).isEqualTo(sameIdDifferentFields);
  }

  @Test
  @DisplayName("equals: given different id, then return false")
  void equals_differentId_returnsFalse() {
    User otherIdUser = User.reconstitute(ID + 1, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);

    assertThat(baseUser).isNotEqualTo(otherIdUser);
  }

  @Test
  @DisplayName("equals: given two transient users with identical fields, then return false")
  void equals_twoTransientUsers_returnsFalseEvenWithIdenticalFields() {
    User transient1 = User.create(NAME, EMAIL, PASSWORD_HASH);
    User transient2 = User.create(NAME, EMAIL, PASSWORD_HASH);

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
    Email otherEmail = ResultAsserts.success(Email.of("other@mail.com"));
    User sameIdDifferentFields =
        User.reconstitute(ID, NAME, otherEmail, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);

    assertThat(baseUser.hashCode()).isEqualTo(sameIdDifferentFields.hashCode());
  }
}
