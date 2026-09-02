package com.anibalxyz.features.auth.domain;

import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN_STRING;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.domain.error.InvalidRawTokenError;
import com.anibalxyz.shared.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RawTokenTest extends UnitTest {
  @Test
  @DisplayName("isValid: given null token, then return false")
  void isValid_nullToken_returnFalse() {
    assertThat(RawToken.isValid(null)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123e4567-e89b-41d4-a716", "123e4567-e89b-41d4-a716-4466554400001"})
  @DisplayName("isValid: given string with length other than 36, then return false")
  void isValid_invalidLength_returnFalse(String invalidLengthToken) {
    assertThat(RawToken.isValid(invalidLengthToken)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "123e4567Xe89b-41d4-a716-446655440000",
        "123e4567-e89bX41d4-a716-446655440000",
        "123e4567-e89b-41d4Xa716-446655440000",
        "123e4567-e89b-41d4-a716X446655440000"
      })
  @DisplayName("isValid: given misplaced hyphens at any required index, then return false")
  void isValid_misplacedHyphens_returnFalse(String invalidHyphenToken) {
    assertThat(RawToken.isValid(invalidHyphenToken)).isFalse();
  }

  @Test
  @DisplayName("isValid: given version character other than '4', then return false")
  void isValid_nonV4Version_returnFalse() {
    String v1Uuid = "123e4567-e89b-11d4-a716-446655440000";
    assertThat(RawToken.isValid(v1Uuid)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(chars = {'0', '7', 'c', 'C', 'z', 'F'})
  @DisplayName("isValid: given invalid variant character, then return false")
  void isValid_invalidVariant_returnFalse(char invalidVariant) {
    char[] chars = VALID_REFRESH_RAW_TOKEN_STRING.toCharArray();
    chars[19] = invalidVariant;
    String invalidVariantToken = new String(chars);

    assertThat(RawToken.isValid(invalidVariantToken)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(chars = {'/', ':', 'G', 'g'})
  @DisplayName("isValid: given non-hexadecimal characters across boundaries, then return false")
  void isValid_nonHexCharacterBoundaries_returnFalse(char invalidHexChar) {
    char[] chars = VALID_REFRESH_RAW_TOKEN_STRING.toCharArray();
    chars[0] = invalidHexChar;
    String nonHexToken = new String(chars);

    assertThat(RawToken.isValid(nonHexToken)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(chars = {'8', '9', 'a', 'b', 'A', 'B'})
  @DisplayName("isValid: given valid UUID v4 string with allowed variant, then return true")
  void isValid_validUuidV4Variants_returnTrue(char validVariant) {
    char[] chars = VALID_REFRESH_RAW_TOKEN_STRING.toCharArray();
    chars[19] = validVariant;
    String validToken = new String(chars);

    assertThat(RawToken.isValid(validToken)).isTrue();
  }

  @Test
  @DisplayName(
      "isValid: given valid UUID v4 with uppercase hex characters in loop body, then return true")
  void isValid_uppercaseHexInLoop_returnTrue() {
    String uppercaseUuid = "123E4567-E89B-41D4-A716-446655440000";
    assertThat(RawToken.isValid(uppercaseUuid)).isTrue();
  }

  @Test
  @DisplayName("of: given invalid raw token string, then return failure error")
  void of_invalidRawToken_returnFailureError() {
    Result<RawToken, InvalidRawTokenError> result = RawToken.of("invalid-token");

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("of: given valid raw token string, then return success with RawToken")
  void of_validRawToken_returnSuccess() {
    Result<RawToken, InvalidRawTokenError> result = RawToken.of(VALID_REFRESH_RAW_TOKEN_STRING);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @DisplayName("generate: when invoked, then return a valid RawToken")
  void generate_whenInvoked_returnValidRawToken() {
    RawToken token = RawToken.generate();

    assertThat(RawToken.isValid(token.value())).isTrue();
  }
}
