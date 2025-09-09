package com.anibalxyz.users.domain;

import org.mindrot.jbcrypt.BCrypt;

public record PasswordHash(String value) {
  public PasswordHash {
    if (value == null || value.isBlank() || !value.startsWith("$2")) {
      throw new IllegalArgumentException("Invalid password hash format.");
    }
  }

  public static PasswordHash generate(String raw) {
    if (raw == null || raw.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
    return new PasswordHash(BCrypt.hashpw(raw, BCrypt.gensalt()));
  }

  public boolean matches(String raw) {
    return BCrypt.checkpw(raw, value);
  }

  @Override
  public String toString() {
    return "********"; // prevents data leaks through logs
  }
}
