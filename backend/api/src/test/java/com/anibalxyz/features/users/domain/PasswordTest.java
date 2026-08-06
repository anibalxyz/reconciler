package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import com.anibalxyz.shared.ResultAsserts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for Password VO")
public class PasswordTest {
  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  @DisplayName("of: given a  blank password, then return InvalidPasswordError with Blank reason")
  public void of_blankPassword_returnInvalidPasswordErrorWithBlankReason(String rawPassword) {
    var result = Password.of(rawPassword);
    var failure = ResultAsserts.failure(result);
    assertThat(failure.getReason()).isInstanceOf(InvalidPasswordError.Reason.Blank.class);
  }

  @Test
  @DisplayName("of: given an absent password, then return InvalidPasswordError with Absent reason")
  public void of_absentPassword_returnInvalidPasswordErrorWithAbsentReason() {
    var result = Password.of(null);
    var failure = ResultAsserts.failure(result);
    assertThat(failure.getReason()).isInstanceOf(InvalidPasswordError.Reason.Absent.class);
  }

  @Test
  @DisplayName(
      "of: given a too short password, then return InvalidPasswordError with TooShort reason")
  public void of_tooShortPassword_returnInvalidPasswordErrorWithTooShortReason() {
    var result = Password.of("s".repeat(Password.MIN_LENGTH - 1));
    var failure = ResultAsserts.failure(result);
    assertThat(failure.getReason()).isInstanceOf(InvalidPasswordError.Reason.TooShort.class);
  }

  @Test
  @DisplayName(
      "of: given a too long password, then return InvalidPasswordError with TooLong reason")
  public void of_tooLongPassword_returnInvalidPasswordErrorWithTooLongReason() {
    var result = Password.of("l".repeat(Password.MAX_LENGTH + 1));
    var failure = ResultAsserts.failure(result);
    assertThat(failure.getReason()).isInstanceOf(InvalidPasswordError.Reason.TooLong.class);
  }

  @Test
  @DisplayName("toString: given any PasswordHash object, then return an asterisks string")
  public void toString_anyPasswordHash_returnAsterisksString() {
    // fragile if final string changes, but we assume it will not
    assertThat(VALID_PASSWORD.toString()).isEqualTo("********");
  }
}
