package com.anibalxyz.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UserTest {

  private static final int BCRYPT_LOG_ROUNDS = 12;

  private static final int ID = 1;
  private static final String NAME = "John Doe";
  private static final Email EMAIL = new Email("email@mail.com");
  private static final PasswordHash PASSWORD_HASH =
      PasswordHash.generate("password1234", BCRYPT_LOG_ROUNDS);
  private static final LocalDateTime TIMESTAMP = LocalDateTime.now();

  private User baseUser;

  @BeforeEach
  void setUp() {
    baseUser = new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
  }

  @Test
  @DisplayName("toString: given a User object, then it should return its string representation")
  public void toString_userObject_returnStringRepresentation() {
    String expected =
"""
User(id=%s, name=%s, email=%s, passwordHash=%s, createdAt=%s, updatedAt=%s)"""
            .formatted(ID, NAME, EMAIL.value(), PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
    String actual = baseUser.toString();

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "name", "email", "passwordHash", "createdAt", "updatedAt"})
  @DisplayName("getters: given a valid User, then they should return the correct property values")
  public void getters_validUser_returnCorrectValue(String propName) {
    switch (propName) {
      case "id":
        assertThat(baseUser.getId()).isEqualTo(ID);
        break;
      case "name":
        assertThat(baseUser.getName()).isEqualTo(NAME);
        break;
      case "email":
        assertThat(baseUser.getEmail()).isEqualTo(EMAIL);
        break;
      case "passwordHash":
        assertThat(baseUser.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        break;
      case "createdAt":
        assertThat(baseUser.getCreatedAt()).isEqualTo(TIMESTAMP);
        break;
      case "updatedAt":
        assertThat(baseUser.getUpdatedAt()).isEqualTo(TIMESTAMP);
        break;
    }
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
        String newName = "New Name";
        userUsingWith = baseUser.withName(newName);
        userUsingConstructor = new User(ID, newName, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
        break;

      case "email":
        Email newEmail = new Email("new@mail.com");
        userUsingWith = baseUser.withEmail(newEmail);
        userUsingConstructor = new User(ID, NAME, newEmail, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
        break;

      case "passwordHash":
        PasswordHash newPasswordHash = PasswordHash.generate("newPassword1234", BCRYPT_LOG_ROUNDS);
        userUsingWith = baseUser.withPasswordHash(newPasswordHash);
        userUsingConstructor = new User(ID, NAME, EMAIL, newPasswordHash, TIMESTAMP, TIMESTAMP);
        break;

      case "createdAt":
        LocalDateTime newCreatedAt = TIMESTAMP.minusDays(1);
        userUsingWith = baseUser.withCreatedAt(newCreatedAt);
        userUsingConstructor = new User(ID, NAME, EMAIL, PASSWORD_HASH, newCreatedAt, TIMESTAMP);
        break;

      case "updatedAt":
        LocalDateTime newUpdatedAt = TIMESTAMP.plusDays(1);
        userUsingWith = baseUser.withUpdatedAt(newUpdatedAt);
        userUsingConstructor = new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, newUpdatedAt);
        break;

      default:
        throw new IllegalArgumentException("Invalid Property Name");
    }

    assertThat(userUsingWith).isEqualTo(userUsingConstructor);
  }

  @Test
  @DisplayName("equals: given two identical User objects, then return true")
  public void equals_equalUsers_returnTrue() {
    User user2 = new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
    assertThat(baseUser).isEqualTo(user2);
  }

  @ParameterizedTest
  @DisplayName("equals: given the same User instance, then return true")
  @ValueSource(strings = {"partial", "full"})
  public void equals_sameUser_returnTrue(String constructor) {
    User user = constructor.equals("partial") ? new User(NAME, EMAIL, PASSWORD_HASH) : baseUser;

    assertThat(user).isEqualTo(user);
  }

  @ParameterizedTest
  @DisplayName("equals: given a different object type, then return false")
  @ValueSource(strings = {"generic", "null"})
  public void equals_invalidObjectAndUser_returnFalse(String objectType) {
    Object object = objectType.equals("generic") ? new Object() : null;
    assertThat(baseUser).isNotEqualTo(object);
  }

  @ParameterizedTest
  @DisplayName("equals: given two User objects with different properties, then return false")
  @ValueSource(strings = {"id", "name", "email", "passwordHash", "createdAt", "updatedAt"})
  public void equals_differentUsers_returnFalse(String propName) {
    User differentUser =
        switch (propName) {
          case "id" -> new User(ID + 1, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
          case "name" -> new User(ID, "Different Name", EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
          case "email" ->
              new User(ID, NAME, new Email("diff@mail.com"), PASSWORD_HASH, TIMESTAMP, TIMESTAMP);
          case "passwordHash" ->
              new User(
                  ID,
                  NAME,
                  EMAIL,
                  PasswordHash.generate("differentPassword", BCRYPT_LOG_ROUNDS),
                  TIMESTAMP,
                  TIMESTAMP);
          case "createdAt" ->
              new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP.plusDays(1), TIMESTAMP);
          case "updatedAt" ->
              new User(ID, NAME, EMAIL, PASSWORD_HASH, TIMESTAMP, TIMESTAMP.plusDays(1));
          default -> throw new IllegalArgumentException("Invalid property name: " + propName);
        };
    assertThat(baseUser).isNotEqualTo(differentUser);
  }
}
