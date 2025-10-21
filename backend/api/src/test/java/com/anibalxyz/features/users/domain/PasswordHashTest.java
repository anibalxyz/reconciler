package com.anibalxyz.features.users.domain;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.anibalxyz.features.users.domain.exception.InvalidPasswordFormatException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class PasswordHashTest {
  private static final int SALT_ROUNDS = 12;
  private static final String HASH_PREFIX = "$2a$" + SALT_ROUNDS + "$";
  private static final String VALID_RAW_PASSWORD = "a-valid-password-123";

  private static Stream<String> provideInvalidHashes() {
    return Stream.of(
        " ",
        "invalid-prefix"
            + "i".repeat(60 - "invalid-prefix".length()), // valid length but invalid prefix
        HASH_PREFIX + "l".repeat(60 - HASH_PREFIX.length() + 1), // too long (total 61)
        HASH_PREFIX + "s".repeat(60 - HASH_PREFIX.length() - 1)); // too short (total 59)
  }

  private static Stream<Arguments> provideInvalidPasswordsAndMessages() {
    return Stream.of(
        Arguments.of(null, "Password cannot be null or empty"),
        Arguments.of("", "Password cannot be null or empty"),
        Arguments.of(" ", "Password cannot be null or empty"),
        Arguments.of("short", "Password must be at least 8 characters long"),
        Arguments.of("l".repeat(73), "Password cannot be longer than 72 characters"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a22.chars.salt.part.b31.chars.hash.part.xxxxxxxxxxxxx",
        "another.22.chars.salt.another.31.chars.hash.part.xxxx"
      })
  @DisplayName("constructor: given a valid hash, then create a PasswordHash object")
  public void constructor_validHash_createsPasswordHashObject(String saltAndHashPart) {
    String validHash = HASH_PREFIX + saltAndHashPart;
    assertDoesNotThrow(() -> new PasswordHash(validHash));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @MethodSource("provideInvalidHashes")
  @DisplayName("constructor: given an invalid hash, then throw IllegalArgumentException")
  public void constructor_invalidHash_throwIllegalArgumentException(String invalidHash) {
    assertThatThrownBy(() -> new PasswordHash(invalidHash))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid password hash format");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1234567890",
        "qwertyuiop",
        "q1w2e3r4t5y6u7i8o9p00p9o",
        "mc28-941pa;lmdf1",
        "][123/132=asa\\dasd",
        "`/=`.0x3ri ,sd ,ac x.c"
      })
  @DisplayName("generate: given a valid raw password, then return a valid PasswordHash object")
  public void generate_validRawPassword_returnsPasswordHashObject(String rawPassword) {
    PasswordHash passwordHash = PasswordHash.generate(rawPassword, SALT_ROUNDS);

    assertThat(passwordHash.value()).startsWith(HASH_PREFIX).hasSize(60);
  }

  @ParameterizedTest
  @MethodSource("provideInvalidPasswordsAndMessages")
  @DisplayName("generate: given an invalid raw password, then throw InvalidPasswordFormatException")
  public void generate_invalidRawPassword_throwInvalidPasswordFormatException(
      String rawPassword, String expectedMessage) {
    assertThatThrownBy(() -> PasswordHash.generate(rawPassword, SALT_ROUNDS))
        .isInstanceOf(InvalidPasswordFormatException.class)
        .hasMessage(expectedMessage);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1234567890",
        "qwertyuiop",
        "mc28-941pa;lmdf1",
        "][123/132=asa\\dasd",
        "`/=`.0x3ri ,sd ,ac x.c",
        VALID_RAW_PASSWORD
      })
  @DisplayName("matches: given a matching raw password, then return true")
  public void matches_givenMatchingRawPassword_returnTrue(String rawPassword) {
    PasswordHash passwordHash = PasswordHash.generate(rawPassword, SALT_ROUNDS);

    assertTrue(passwordHash.matches(rawPassword));
  }

  @Test
  @DisplayName("matches: given a non-matching raw password, then return false")
  public void matches_givenNonMatchingRawPassword_returnFalse() {
    PasswordHash passwordHash = PasswordHash.generate(VALID_RAW_PASSWORD, SALT_ROUNDS);

    assertFalse(passwordHash.matches("wrong-password"));
  }

  @Test
  @DisplayName("toString: given any PasswordHash object, then return an asterisks string")
  public void toString_anyPasswordHash_returnAsterisksString() {
    PasswordHash passwordHash = PasswordHash.generate(VALID_RAW_PASSWORD, SALT_ROUNDS);

    assertThat(passwordHash.toString()).isEqualTo("********");
  }
}
