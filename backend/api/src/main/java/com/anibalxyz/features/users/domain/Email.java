package com.anibalxyz.features.users.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a user's email address as an immutable value object.
 *
 * <p>Instances can only be created via {@link #of(String)}, which validates and normalizes the
 * value before constructing the object.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * Result<Email, InvalidEmailError> result = Email.of("user@example.com");
 *
 * if (result.isFailure()) {
 *     // handle invalid email
 * }
 * // use valid email
 * Email email = result.getValue();
 *
 * }</pre>
 */
public final class Email {
  // TODO: check if they are correct being public
  public static final String PATTERN = "^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$";
  public static final int MAX_LENGTH = 255;
  private static final Pattern EMAIL_PATTERN = Pattern.compile(PATTERN);

  private final String value;

  private Email(String value) {
    this.value = value;
  }

  /**
   * @param value the raw email string
   * @return a successful {@link Result} with the normalized {@link Email}, or a failed {@link
   *     Result} with an {@link InvalidEmailError} if the value is invalid
   */
  public static Result<Email, InvalidEmailError> of(String value) {
    Result<Void, InvalidEmailError> validationResult = validateRaw(value);
    if (validationResult.isFailure()) return Result.failure(validationResult.getError());

    return Result.success(new Email(normalize(value)));
  }

  // TODO: rename to validate()
  public static Result<Void, InvalidEmailError> validateRaw(String value) {
    if (value == null) return Result.failure(InvalidEmailError.absent());
    if (value.isBlank()) return Result.failure(InvalidEmailError.blank());
    if (!hasValidFormat(value)) return Result.failure(InvalidEmailError.invalidFormat());

    return Result.success(null);
  }

  /**
   * @return normalized email string by converting it to lowercase and trimming whitespace.
   */
  public static String normalize(String email) {
    return email.toLowerCase(Locale.ROOT).trim();
  }

  private static boolean hasValidFormat(String email) {
    // Format and length are validated together for now — this may change in the future.
    // TODO: basic validation first
    return EMAIL_PATTERN.matcher(email).matches() && email.length() <= MAX_LENGTH;
  }

  public String value() {
    return value;
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
    if (!(o instanceof Email other)) return false;
    return Objects.equals(value, other.value());
  }

  @Override
  @ExcludeFromJacocoGenerated
  public String toString() {
    return value;
  }
}
