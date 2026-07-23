package com.anibalxyz.features.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for Email Value Object")
public class EmailTest {

  @ParameterizedTest
  @ValueSource(strings = {"valid@mail.com", "a@mail.uy", "vAl1d.e-mail@domain.ar"})
  @DisplayName("of: given a valid email, then return a successful Result")
  public void of_validEmail_returnSuccess(String validEmailString) {
    Result<Email, InvalidEmailError> result = Email.of(validEmailString);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue().value()).isEqualTo(Email.normalize(validEmailString));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  @DisplayName("of: given a blank email, then return a failed Result with Blank reason")
  public void of_blankEmail_returnFailureWithBlank(String blank) {
    Result<Email, InvalidEmailError> result = Email.of(blank);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason()).isInstanceOf(InvalidEmailError.Reason.Blank.class);
  }

  @Test
  @DisplayName("of: given an absent email, then return a failed Result with Absent reason")
  public void of_absentEmail_returnFailureWithAbsent() {
    Result<Email, InvalidEmailError> result = Email.of(null);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason()).isInstanceOf(InvalidEmailError.Reason.Absent.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plainaddress",
        "#@%^%#$@#$@#.com",
        "@example.com",
        "email.example.com",
        "email@example@com",
        "lengthGT255qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqw@mail.com"
      })
  @DisplayName(
      "of: given an invalid email format, then return a failed Result with InvalidFormat reason")
  public void of_invalidEmailFormat_returnFailureWithInvalidFormat(String invalidEmailString) {
    Result<Email, InvalidEmailError> result = Email.of(invalidEmailString);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason())
        .isInstanceOf(InvalidEmailError.Reason.InvalidFormat.class);
  }

  @Test
  @DisplayName(
      "of: given an email with uppercase letters, then return the email normalized to lowercase")
  public void of_uppercaseEmail_returnNormalizedToLowerCase() {
    String uppercaseEmail = "ExampleEMAIL@Domain.COM";
    Result<Email, InvalidEmailError> email = Email.of(uppercaseEmail);
    assertThat(email.isSuccess()).isTrue();
    assertThat(email.getValue().value()).isEqualTo(uppercaseEmail.toLowerCase());
  }
}
