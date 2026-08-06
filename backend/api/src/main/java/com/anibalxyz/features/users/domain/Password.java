package com.anibalxyz.features.users.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * A validated plain-text password, transient by design.
 *
 * <p>Only lives during request processing (commands, use cases). Never persist or log the raw
 * value; once hashed via {@link PasswordHash#of(Password, int)}, keep only the hash.
 */
public class Password {
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 72;

  private final String value;

  private Password(String value) {
    this.value = value;
  }

  public static Result<Password, InvalidPasswordError> of(String raw) {
    return validate(raw).map(v -> new Password(raw));
  }

  public static Result<Void, InvalidPasswordError> validate(String raw) {
    if (raw == null) return Result.failure(InvalidPasswordError.absent());
    if (raw.isBlank()) return Result.failure(InvalidPasswordError.blank());
    if (raw.length() < MIN_LENGTH) return Result.failure(InvalidPasswordError.tooShort(MIN_LENGTH));
    if (raw.length() > MAX_LENGTH) return Result.failure(InvalidPasswordError.tooLong(MAX_LENGTH));

    return Result.success();
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
    if (!(o instanceof Password other)) return false;
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
