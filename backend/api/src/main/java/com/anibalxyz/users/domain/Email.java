package com.anibalxyz.users.domain;

import java.util.regex.Pattern;

public record Email(String value) {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$");

  public Email {
    if (!isValid(value)) {
      throw new IllegalArgumentException("Invalid email format: " + value);
    }
  }

  public static boolean isValid(String email) {
    return (email != null
        && !email.isBlank()
        && EMAIL_PATTERN.matcher(email).matches()
        && email.length() <= 255);
  }
}
