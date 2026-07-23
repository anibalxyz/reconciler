package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.core.domain.error.DomainErrorReason;
import com.anibalxyz.core.domain.error.ReasonedError;

/**
 * Domain error representing an invalid password value.
 *
 * <p>Use {@link #getReason()} with pattern matching to handle specific cases:
 *
 * <pre>{@code
 * switch (error.getReason()) {
 *     case TooShort r -> "Must be at least " + r.minLength() + " characters";
 *     case TooLong r  -> "Must be at most " + r.maxLength() + " characters";
 *     case Blank r    -> "Cannot be empty";
 * }
 * }</pre>
 */
public final class InvalidPasswordError extends ReasonedError<InvalidPasswordError.Reason>
    implements UserDomainError.InvalidValueError {

  private InvalidPasswordError(Reason reason) {
    super(reason);
  }

  public static InvalidPasswordError blank() {
    return new InvalidPasswordError(new Reason.Blank());
  }

  public static InvalidPasswordError absent() {
    return new InvalidPasswordError(new Reason.Absent());
  }

  public static InvalidPasswordError tooShort(int minLength) {
    return new InvalidPasswordError(new Reason.TooShort(minLength));
  }

  public static InvalidPasswordError tooLong(int maxLength) {
    return new InvalidPasswordError(new Reason.TooLong(maxLength));
  }

  public sealed interface Reason extends DomainErrorReason {
    record Blank() implements Reason {}

    record Absent() implements Reason {}

    record TooShort(int minLength) implements Reason {}

    record TooLong(int maxLength) implements Reason {}
  }
}
