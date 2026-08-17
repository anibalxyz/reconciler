package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.anibalxyz.features.users.domain.error.InvalidPasswordHashError;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for PasswordHash VO")
public class PasswordHashTest extends UnitTest {
  public static int BCRYPT_LOG_ROUNDS;
  private static String HASH_PREFIX;

  @BeforeAll
  public static void setup() {
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
  @DisplayName("reconstitute: given a valid hash, then create a PasswordHash object")
  public void reconstitute_validHash_createsPasswordHashObject(String saltAndHashPart) {
    var result = PasswordHash.reconstitute(HASH_PREFIX + saltAndHashPart);
    var hash = ResultAsserts.success(result);
    assertThat(hash.value()).isEqualTo(HASH_PREFIX + saltAndHashPart);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @MethodSource("provideInvalidHashes")
  @DisplayName("reconstitute: given an invalid hash, then throw InvalidPasswordHashException")
  public void reconstitute_invalidHash_throwInvalidPasswordHashException(String invalidHash) {
    var result = PasswordHash.reconstitute(invalidHash);
    var failure = ResultAsserts.failure(result);
    assertThat(failure).isInstanceOf(InvalidPasswordHashError.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1234567890",
        "qwertyuiop",
        "mc28-941pa;lmdf1",
        "][123/132=asa\\dasd",
        "`/=`.0x3ri ,sd ,ac x.c",
        VALID_PASSWORD_STRING
      })
  @DisplayName("matches: given a matching raw password, then return true")
  public void matches_givenMatchingRawPassword_returnTrue(String rawPassword) {
    var validated = ResultAsserts.success(Password.of(rawPassword));
    var hash = PasswordHash.of(validated, BCRYPT_LOG_ROUNDS);
    assertThat(hash.matches(rawPassword)).isTrue();
  }

  @Test
  @DisplayName("matches: given a non-matching raw password, then return false")
  public void matches_givenNonMatchingRawPassword_returnFalse() {
    assertFalse(VALID_PASSWORD_HASH.matches("other" + VALID_PASSWORD_STRING));
  }

  @Test
  @DisplayName("toString: given any PasswordHash object, then return an asterisks string")
  public void toString_anyPasswordHash_returnAsterisksString() {
    // fragile if final string changes, but we assume it will not
    assertThat(VALID_PASSWORD_HASH.toString()).isEqualTo("********");
  }
}
