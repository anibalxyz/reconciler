package com.anibalxyz.users.domain;

import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

// TODO: throw more specific and user friendly exceptions
public record PasswordHash(String value) {
  private static final Pattern BCRYPT_PATTERN =
      Pattern.compile("\\A\\$2a\\$\\d\\d\\$[./0-9A-Za-z]{53}");

  public PasswordHash {
    if (!isValidHash(value)) {
      throw new IllegalArgumentException("Invalid password hash format.");
    }
  }

  public static boolean isValidHash(String hash) {
    return hash != null && BCRYPT_PATTERN.matcher(hash).matches();
  }

  public static boolean isValidRaw(String raw) {
    return (raw != null && !raw.isBlank() && raw.length() >= 8 && raw.length() <= 72);
  }

  public static PasswordHash generate(String raw, int saltRounds) {
    if (!isValidRaw(raw)) {
      throw new IllegalArgumentException("Password must be between 8 and 72 characters.");
    }
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
