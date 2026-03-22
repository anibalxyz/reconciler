package com.anibalxyz.features.users.domain;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a user's email address as an immutable value object.
 *
 * <p>Instances can only be created via {@link #of(String)}, which validates and normalizes the
 * value before constructing the object. The constructor is private to ensure no invalid {@code
 * Email} can exist.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * Result<Email, InvalidEmailError> result = Email.of("user@example.com");
 *
 * if (result.isFailure()) {
 *     // handle invalid email
 * } else {
 *     Email email = result.getValue();
 * }
 * }</pre>
 */
public final class Email {

  public static final String PATTERN = "^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$";
  public static final int MAX_LENGTH = 255;
  private static final Pattern EMAIL_PATTERN = Pattern.compile(PATTERN);

  private final String value;

  private Email(String value) {
    this.value = value;
  }

  /**
   * Creates a new {@code Email} from the given string value.
   *
   * <p>Validates the format and length, then normalizes (lowercase + trim) before constructing the
   * object. Returns a {@link Result} so callers can handle the failure without catching an
   * exception, enabling accumulation of multiple field errors.
   *
   * @param value the raw email string
   * @return a successful {@code Result} with the normalized {@code Email}, or a failed {@code
   *     Result} with an {@link InvalidEmailError} if the value is invalid
   */
  public static Result<Email, InvalidEmailError> of(String value) {
    if (!isValid(value)) {
      return Result.failure(new InvalidEmailError(new InvalidEmailError.Reason.InvalidFormat()));
    }
    return Result.success(new Email(normalize(value)));
  }

  /**
   * Returns the string representation of this email address.
   *
   * @return the normalized email value
   */
  public String value() {
    return value;
  }

  /**
   * Normalizes an email string by converting it to lowercase and trimming whitespace.
   *
   * @param email the email string to normalize
   * @return the normalized email string
   */
  public static String normalize(String email) {
    return email.toLowerCase(Locale.ROOT).trim();
  }

  /**
   * Checks whether a given string is a valid email.
   *
   * <p>Private — validation is encapsulated within {@link #of(String)}.
   *
   * @param email the email string to validate
   * @return {@code true} if valid, {@code false} otherwise
   */
  private static boolean isValid(String email) {
    return email != null
        && !email.isBlank()
        && EMAIL_PATTERN.matcher(email).matches()
        && email.length() <= MAX_LENGTH;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Email other)) return false;
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
