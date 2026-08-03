package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.core.domain.error.DomainErrorReason;
import com.anibalxyz.core.domain.error.ReasonedError;

/**
 * Domain error representing an invalid email value.
 *
 * <p>Use {@link #getReason()} with pattern matching to handle specific cases:
 *
 * <pre>{@code
 * switch (error.getReason()) {
 *     case InvalidFormat r -> "Invalid email format";
 * }
 * }</pre>
 */
public final class InvalidNameError extends ReasonedError<InvalidNameError.Reason>
    implements UserDomainError.InvalidValueError {

  private InvalidNameError(Reason reason) {
    super(reason);
  }

  public static InvalidNameError tooLong(int maxLength) {
    return new InvalidNameError(new Reason.TooLong(maxLength));
  }

  public static InvalidNameError blank() {
    return new InvalidNameError(new Reason.Blank());
  }

  public static InvalidNameError absent() {
    return new InvalidNameError(new Reason.Absent());
  }

  public sealed interface Reason extends DomainErrorReason {
    record TooLong(int maxLength) implements Reason {}

    record Blank() implements Reason {}

    record Absent() implements Reason {}
  }
}
