package com.anibalxyz.users.domain;

import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

public record PasswordHash(String value) {
  public PasswordHash {
    if (!isValidHash(value)) {
      throw new IllegalArgumentException("Invalid password hash format.");
    }
  }

  public static boolean isValidHash(String hash) {
    return (hash != null && !hash.isBlank() && hash.startsWith("$2a$10$"));
  }

  public static boolean isValidRaw(String raw) {
    // TODO: add more business rules/validations
    return (raw != null && !raw.isBlank() && raw.length() >= 8);
  }

  public static PasswordHash generate(String raw) {
    if (!isValidRaw(raw)) {
      throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
    return new PasswordHash(BCrypt.hashpw(raw, BCrypt.gensalt()));
  }

  public boolean matches(String raw) {
    return BCrypt.checkpw(raw, value);
  }

  @NotNull
  @Override
  public String toString() {
    return "********"; // prevents data leaks through logs
  }
}
