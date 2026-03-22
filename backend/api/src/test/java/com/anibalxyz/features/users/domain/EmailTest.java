package com.anibalxyz.features.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for Email Value Object")
public class EmailTest {

  @ParameterizedTest
  @ValueSource(strings = {"valid@mail.com", "a@mail.uy", "vAl1d.e-mail@domain.ar"})
  @DisplayName("of: given a valid email, then return a successful Result")
  public void of_validEmail_returnsSuccess(String validEmailString) {
    assertThat(Email.of(validEmailString).isSuccess()).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "",
        " ",
        "plainaddress",
        "#@%^%#$@#$@#.com",
        "@example.com",
        "email.example.com",
        "email@example@com",
        "lengthGT255qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnmqw@mail.com"
      })
  @DisplayName("of: given an invalid email, then return a failed Result with InvalidFormat reason")
  public void of_invalidEmail_returnsFailureWithInvalidFormat(String invalidEmailString) {
    var result = Email.of(invalidEmailString);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError().getReason())
        .isInstanceOf(InvalidEmailError.Reason.InvalidFormat.class);
  }

  @Test
  @DisplayName(
      "of: given an email with uppercase letters, then return the email normalized to lowercase")
  public void of_uppercaseEmail_returnsNormalizedToLowerCase() {
    String uppercaseEmail = "ExampleEMAIL@Domain.COM";
    Email email = Email.of(uppercaseEmail).getValue();
    assertThat(email.value()).isEqualTo(uppercaseEmail.toLowerCase());
  }
}
