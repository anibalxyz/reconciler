package com.anibalxyz.features.users.domain;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Represents a hashed password as an immutable value object.
 *
 * <p>This class encapsulates the logic for creating, validating, and verifying password hashes
 * using the BCrypt algorithm. It ensures that plain-text passwords are never stored or passed
 * around the domain, and prevents accidental logging of the hash value.
 *
 * <p>Plain-text passwords are validated and hashed via {@link #generate(String, int)}, which
 * returns a {@link Result} to allow callers to handle validation failures without catching
 * exceptions, enabling accumulation of multiple field errors.
 *
 * @param value The BCrypt hashed password string.
 */
public record PasswordHash(String value) {
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 72;
  private static final Pattern BCRYPT_PATTERN =
      Pattern.compile("\\A\\$2a\\$\\d\\d\\$[./0-9A-Za-z]{53}");

  /**
   * Compact constructor for the {@code PasswordHash} record. Validates that the provided value is a
   * valid BCrypt hash, ensuring no invalid hash can exist.
   *
   * @throws IllegalArgumentException if the hash format is invalid
   */
  public PasswordHash {
    if (!isValidHash(value)) {
      throw new IllegalArgumentException("Invalid password hash format");
    }
  }

  /**
   * Creates a new {@code PasswordHash} from a plain-text password.
   *
   * <p>Validates the password against complexity requirements, then salts and hashes it using
   * BCrypt. Returns a {@link Result} so callers can handle validation failures without catching
   * exceptions.
   *
   * @param raw the plain-text password to hash
   * @param saltRounds the log2 of the number of BCrypt hashing rounds
   * @return a successful {@code Result} with the hashed password, or a failed {@code Result} with
   *     an {@link InvalidPasswordError} if validation fails
   */
  public static Result<PasswordHash, InvalidPasswordError> generate(String raw, int saltRounds) {
    if (raw == null || raw.isBlank()) return Result.failure(InvalidPasswordError.empty());
    if (raw.length() < MIN_LENGTH) return Result.failure(InvalidPasswordError.tooShort(MIN_LENGTH));
    if (raw.length() > MAX_LENGTH) return Result.failure(InvalidPasswordError.tooLong(MAX_LENGTH));

    return Result.success(new PasswordHash(BCrypt.hashpw(raw, BCrypt.gensalt(saltRounds))));
  }

  /**
   * Validates if a string conforms to the BCrypt hash format.
   *
   * @param hash the hash string to validate
   * @return {@code true} if the hash is valid, {@code false} otherwise
   */
  public static boolean isValidHash(String hash) {
    return hash != null && BCRYPT_PATTERN.matcher(hash).matches();
  }

  /**
   * Verifies a plain-text password against the stored hash.
   *
   * @param raw the plain-text password to check
   * @return {@code true} if the password matches the hash, {@code false} otherwise
   */
  public boolean matches(String raw) {
    return BCrypt.checkpw(raw, value);
  }

  /**
   * Overrides toString to prevent leaking the hash value in logs.
   *
   * @return a fixed-string mask
   */
  @NotNull
  @Override
  public String toString() {
    return "********";
  }
}
