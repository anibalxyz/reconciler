package com.anibalxyz.features.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.users.domain.error.InvalidNameError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for Name Value Object")
public class NameTest {

  @ParameterizedTest
  @ValueSource(strings = {"Name", "Some other name", "There are no format rules yet hehe"})
  @DisplayName("of: given a valid name, then return a successful Result")
  public void of_validName_returnSuccess(String name) {
    Result<Name, InvalidNameError> result = Name.of(name);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue().value()).isEqualTo(name);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  @DisplayName("of: given a blank name, then return a failed Result with Blank reason")
  public void of_blankName_returnFailureWithBlank(String blank) {
    Result<Name, InvalidNameError> result = Name.of(blank);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason()).isInstanceOf(InvalidNameError.Reason.Blank.class);
  }

  @Test
  @DisplayName("of: given a absent name, then return a failed Result with Absent reason")
  public void of_absentName_returnFailureWithAbsent() {
    Result<Name, InvalidNameError> result = Name.of(null);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason()).isInstanceOf(InvalidNameError.Reason.Absent.class);
  }
}
