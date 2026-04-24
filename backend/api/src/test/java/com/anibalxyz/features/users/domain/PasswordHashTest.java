package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import com.anibalxyz.features.users.domain.error.InvalidPasswordHashError;
import com.anibalxyz.shared.Constants;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for PasswordHash Value Object")
public class PasswordHashTest {
  public static int BCRYPT_LOG_ROUNDS;

  private static String HASH_PREFIX;

  @BeforeAll
  public static void setup() {
    Constants.init();
    BCRYPT_LOG_ROUNDS = Constants.APP_ENV.BCRYPT_LOG_ROUNDS();
    HASH_PREFIX = String.format("$2a$%02d$", BCRYPT_LOG_ROUNDS);
  }

  private static Stream<String> provideInvalidHashes() {
    return Stream.of(
        " ",
        "invalid-prefix"
            + "i".repeat(60 - "invalid-prefix".length()), // valid length but invalid prefix
        HASH_PREFIX + "l".repeat(60 - HASH_PREFIX.length() + 1), // too long (total 61)
        HASH_PREFIX + "s".repeat(60 - HASH_PREFIX.length() - 1)); // too short (total 59)
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
    Result<PasswordHash, InvalidPasswordHashError> result = PasswordHash.of(validHash);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue().value()).isEqualTo(validHash);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @MethodSource("provideInvalidHashes")
  @DisplayName("of: given an invalid hash, then throw InvalidPasswordHashException")
  public void of_invalidHash_throwInvalidPasswordHashException(String invalidHash) {
    Result<PasswordHash, InvalidPasswordHashError> result = PasswordHash.of(invalidHash);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isInstanceOf(InvalidPasswordHashError.class);
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
  public void generate_validRawPassword_returnPasswordHashObject(String rawPassword) {
    Result<PasswordHash, InvalidPasswordError> passwordHash =
        PasswordHash.generate(rawPassword, BCRYPT_LOG_ROUNDS);

    assertThat(passwordHash.isSuccess()).isTrue();
    assertThat(passwordHash.getValue().value()).startsWith(HASH_PREFIX).hasSize(60);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  @DisplayName(
      "generate: given a  blank password, then return InvalidPasswordError with Blank reason")
  public void generate_blankPassword_returnInvalidPasswordErrorWithBlankReason(String rawPassword) {
    Result<PasswordHash, InvalidPasswordError> result =
        PasswordHash.generate(rawPassword, BCRYPT_LOG_ROUNDS);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason()).isInstanceOf(InvalidPasswordError.Reason.Blank.class);
  }

  @Test
  @DisplayName(
      "generate: given an absent password, then return InvalidPasswordError with Absent reason")
  public void generate_absentPassword_returnInvalidPasswordErrorWithAbsentReason() {
    Result<PasswordHash, InvalidPasswordError> result =
        PasswordHash.generate(null, BCRYPT_LOG_ROUNDS);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason())
        .isInstanceOf(InvalidPasswordError.Reason.Absent.class);
  }

  @Test
  @DisplayName(
      "generate: given a too short password, then return InvalidPasswordError with TooShort reason")
  public void generate_tooShortPassword_returnInvalidPasswordErrorWithTooShortReason() {
    Result<PasswordHash, InvalidPasswordError> result =
        PasswordHash.generate("short", BCRYPT_LOG_ROUNDS);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason())
        .isInstanceOf(InvalidPasswordError.Reason.TooShort.class);
  }

  @Test
  @DisplayName(
      "generate: given a too long password, then return InvalidPasswordError with TooLong reason")
  public void generate_tooLongPassword_returnInvalidPasswordErrorWithTooLongReason() {
    Result<PasswordHash, InvalidPasswordError> result =
        PasswordHash.generate("l".repeat(73), BCRYPT_LOG_ROUNDS);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason())
        .isInstanceOf(InvalidPasswordError.Reason.TooLong.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1234567890",
        "qwertyuiop",
        "mc28-941pa;lmdf1",
        "][123/132=asa\\dasd",
        "`/=`.0x3ri ,sd ,ac x.c",
        VALID_PASSWORD
      })
  @DisplayName("matches: given a matching raw password, then return true")
  public void matches_givenMatchingRawPassword_returnTrue(String rawPassword) {
    Result<PasswordHash, InvalidPasswordError> passwordHash =
        PasswordHash.generate(rawPassword, BCRYPT_LOG_ROUNDS);

    assertThat(passwordHash.isSuccess()).isTrue();
    assertTrue(passwordHash.getValue().matches(rawPassword));
  }

  @Test
  @DisplayName("matches: given a non-matching raw password, then return false")
  public void matches_givenNonMatchingRawPassword_returnFalse() {
    Result<PasswordHash, InvalidPasswordError> passwordHash =
        PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS);

    assertThat(passwordHash.isSuccess()).isTrue();
    assertFalse(passwordHash.getValue().matches("wrong-password"));
  }

  @Test
  @DisplayName("toString: given any PasswordHash object, then return an asterisks string")
  public void toString_anyPasswordHash_returnAsterisksString() {
    Result<PasswordHash, InvalidPasswordError> passwordHash =
        PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS);

    assertThat(passwordHash.isSuccess()).isTrue();
    assertThat(passwordHash.getValue().toString()).isEqualTo("********");
  }
}
