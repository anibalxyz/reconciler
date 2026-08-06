package com.anibalxyz.features.users.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.error.InvalidPasswordHashError;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

/**
 * A hashed password as an immutable value object.
 *
 * <p>Owns the BCrypt hash: produced from an already-validated password via {@link #of(Password,
 * int)} and rebuilt from persistence via {@link #reconstitute(String)}. Raw password
 * formatting/validation lives in {@link Password}.
 */
public class PasswordHash {
  private static final Pattern BCRYPT_PATTERN =
      Pattern.compile("\\A\\$2a\\$\\d\\d\\$[./0-9A-Za-z]{53}");

  private final String value;

  private PasswordHash(String value) {
    this.value = value;
  }

  /**
   * Factory method for existing hashes (e.g., from the database).
   *
   * @return success with {@link PasswordHash}, or failure if the format is invalid.
   */
  public static Result<PasswordHash, InvalidPasswordHashError> reconstitute(String hash) {
    return isValid(hash)
        ? Result.success(new PasswordHash(hash))
        : Result.failure(new InvalidPasswordHashError());
  }

  /**
   * @return {@code true} if {@code hash} conforms to the BCrypt hash format.
   */
  public static boolean isValid(String hash) {
    return hash != null && BCRYPT_PATTERN.matcher(hash).matches();
  }

  /**
   * Hashes the already-validated password using BCrypt.
   *
   * @param password the validated raw password
   * @param saltRounds the log2 of the number of BCrypt hashing rounds
   * @return the corresponding {@code PasswordHash}
   */
  public static PasswordHash of(Password password, int saltRounds) {
    return new PasswordHash(BCrypt.hashpw(password.value(), BCrypt.gensalt(saltRounds)));
  }

  /**
   * @return {@code true} if plain-text {@code password} matches the stored hash.
   */
  public boolean matches(String password) {
    return BCrypt.checkpw(password, value);
  }

  @Override
  @ExcludeFromJacocoGenerated
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  @ExcludeFromJacocoGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PasswordHash other)) return false;
    return Objects.equals(value, other.value());
  }

  public String value() {
    return value;
  }

  /**
   * @return a masked string to prevent accidental leaks in logs.
   */
  @NotNull
  @Override
  public String toString() {
    return "********";
  }
}
