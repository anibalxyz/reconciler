package com.anibalxyz.features.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Email Value Object Tests")
public class EmailTest {

  @ParameterizedTest
  @ValueSource(strings = {"valid@mail.com", "a@mail.uy", "vAl1d.e-mail@domain.ar"})
  @DisplayName("constructor: given a valid email, then create an Email object")
  public void constructor_validEmail_returnEmailObject(String validEmailString) {
    assertDoesNotThrow(
        () -> {
          Email email = new Email(validEmailString);
        });
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
  @DisplayName("constructor: given an invalid email format, then throw IllegalArgumentException")
  public void constructor_invalidEmail_throwsIllegalArgumentException(String invalidEmailString) {
    assertThatThrownBy(() -> new Email(invalidEmailString))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email format");
  }

  @Test
  @DisplayName(
      "constructor: given an email with uppercase letters, then return the email normalized to lowercase")
  public void constructor_uppercaseEmail_returnNormalizedToLowerCaseEmail() {
    String uppercaseEmail = "ExampleEMAIL@Domain.COM";
    Email email = new Email(uppercaseEmail);
    assertThat(email.value()).isEqualTo(uppercaseEmail.toLowerCase());
  }
}
