package com.anibalxyz.features.users.domain;

import static com.anibalxyz.shared.Constants.Users.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.domain.error.InvalidUserIdError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for UserId Value Object")
public class UserIdTest extends UnitTest {
  @ParameterizedTest
  @ValueSource(ints = {0, -1, -100})
  @DisplayName("of: given zero or negative integer, then return failure with InvalidUserIdError")
  void of_zeroOrNegative_returnFailure(int value) {
    var result = UserId.of(value);

    assertThat(ResultAsserts.failure(result)).isInstanceOf(InvalidUserIdError.class);
  }

  @Test
  @DisplayName("of: given null, then return failure with InvalidUserIdError")
  void of_null_returnFailure() {
    var result = UserId.of(null);

    assertThat(ResultAsserts.failure(result)).isInstanceOf(InvalidUserIdError.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 42, 1000})
  @DisplayName("of: given a positive integer, then return success with UserId")
  void of_positiveInteger_returnSuccess(int value) {
    var result = UserId.of(value);

    assertThat(ResultAsserts.success(result).value()).isEqualTo(value);
  }
}
