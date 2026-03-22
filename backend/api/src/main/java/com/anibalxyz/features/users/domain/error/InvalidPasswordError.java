package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.features.common.domain.error.DomainErrorReason;
import com.anibalxyz.features.common.domain.error.ReasonedError;

/**
 * Domain error representing an invalid password value.
 *
 * <p>Use {@link #getReason()} with pattern matching to handle specific cases:
 *
 * <pre>{@code
 * switch (error.getReason()) {
 *     case TooShort r -> "Must be at least " + r.minLength() + " characters";
 *     case TooLong r  -> "Must be at most " + r.maxLength() + " characters";
 *     case Empty r    -> "Cannot be empty";
 * }
 * }</pre>
 */
public final class InvalidPasswordError extends ReasonedError<InvalidPasswordError.Reason>
    implements UserDomainError {

  public sealed interface Reason extends DomainErrorReason {
    record Empty() implements Reason {}

    record TooShort(int minLength) implements Reason {}

    record TooLong(int maxLength) implements Reason {}
  }

  public InvalidPasswordError(Reason reason) {
    super(reason);
  }

  public static InvalidPasswordError empty() {
    return new InvalidPasswordError(new Reason.Empty());
  }

  public static InvalidPasswordError tooShort(int minLength) {
    return new InvalidPasswordError(new Reason.TooShort(minLength));
  }

  public static InvalidPasswordError tooLong(int maxLength) {
    return new InvalidPasswordError(new Reason.TooLong(maxLength));
  }
}
