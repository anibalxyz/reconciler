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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    baseUser = new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
  }

  @Test
  @DisplayName(
      "constructor: given name, email and passwordHash, then creates user with null id and timestamps")
  public void partialConstructor_createsUserWithNullIdAndTimestamps() {
    User actual = new User(NAME, EMAIL, PASSWORD_HASH);
    User expected = new User(null, NAME, EMAIL, PASSWORD_HASH, null, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("toString: given a User object, then it should return its string representation")
  public void toString_userObject_returnStringRepresentation() {
    String expected =
"""
User(id=%s, name=%s, email=%s, passwordHash=%s, createdAt=%s, updatedAt=%s)"""
            .formatted(ID, NAME.value(), EMAIL.value(), PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
    String actual = baseUser.toString();

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "name", "email", "passwordHash", "createdAt", "updatedAt"})
  @DisplayName(
      "with-methods: given a User, then they should create a new instance with the updated value")
  public void withMethods_createNewInstanceWithUpdatedValue(String propName) {

    User userUsingWith;
    User userUsingConstructor;

    switch (propName) {
      case "id":
        int newId = 2;
        userUsingWith = baseUser.withId(newId);
        userUsingConstructor = new User(newId, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
        break;

      case "name":
        Name newName = ResultAsserts.success(Name.of("New Name"));
        userUsingWith = baseUser.withName(newName);
        userUsingConstructor = new User(ID, newName, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
        break;

      case "email":
        Email newEmail = ResultAsserts.success(Email.of("new@mail.com"));
        userUsingWith = baseUser.withEmail(newEmail);
        userUsingConstructor = new User(ID, NAME, newEmail, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
        break;

      case "passwordHash":
        PasswordHash newPasswordHash =
            ResultAsserts.success(PasswordHash.generate("newPassword1234", BCRYPT_LOG_ROUNDS));
        userUsingWith = baseUser.withPasswordHash(newPasswordHash);
        userUsingConstructor = new User(ID, NAME, EMAIL, newPasswordHash, TIMESTAMP, TIMESTAMP);
        break;

      case "createdAt":
        Instant newCreatedAt = TIMESTAMP.minusSeconds(60 * 60 * 24);
        userUsingWith = baseUser.withCreatedAt(newCreatedAt);
        userUsingConstructor = new User(ID, NAME, EMAIL, PASSWORD_HASH, newCreatedAt, TIMESTAMP);
        break;

      case "updatedAt":
        Instant newUpdatedAt = TIMESTAMP.plusSeconds(60 * 60 * 24);
        userUsingWith = baseUser.withUpdatedAt(newUpdatedAt);
        userUsingConstructor = new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, newUpdatedAt);
        break;

      default:
        throw new IllegalArgumentException("Invalid Property Name");
    }

    assertThat(userUsingWith).isEqualTo(userUsingConstructor);
  }
}
