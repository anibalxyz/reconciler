package com.anibalxyz.users.domain;

import java.util.regex.Pattern;

public record Email(String value) {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$");

  public Email {
    if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid email format: " + value);
    }
  }
}
