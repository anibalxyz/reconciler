package com.anibalxyz.users.domain;

import com.anibalxyz.users.domain.exception.InvalidPasswordFormatException;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

public record PasswordHash(String value) {
  private static final Pattern BCRYPT_PATTERN =
      Pattern.compile("\\A\\$2a\\$\\d\\d\\$[./0-9A-Za-z]{53}");

  public PasswordHash {
    if (!isValidHash(value)) {
      throw new IllegalArgumentException("Invalid password hash format");
    }
  }

  public static boolean isValidHash(String hash) {
    return hash != null && BCRYPT_PATTERN.matcher(hash).matches();
  }

  private static void validateRawPassword(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new InvalidPasswordFormatException("Password cannot be null or empty");
    }
    if (raw.length() < 8) {
      throw new InvalidPasswordFormatException("Password must be at least 8 characters long");
    }
    if (raw.length() > 72) {
      throw new InvalidPasswordFormatException("Password cannot be longer than 72 characters");
    }
  }

  public static PasswordHash generate(String raw, int saltRounds) {
    validateRawPassword(raw);
    return new PasswordHash(BCrypt.hashpw(raw, BCrypt.gensalt(saltRounds)));
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
