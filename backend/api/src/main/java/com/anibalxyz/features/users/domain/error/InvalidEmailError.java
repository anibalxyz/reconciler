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
public final class InvalidEmailError extends ReasonedError<InvalidEmailError.Reason>
    implements UserDomainError.InvalidValueError {

  private InvalidEmailError(Reason reason) {
    super(reason);
  }

  public static InvalidEmailError invalidFormat() {
    return new InvalidEmailError(new Reason.InvalidFormat());
  }

  public static InvalidEmailError tooLong(int maxLength) {
    return new InvalidEmailError(new Reason.TooLong(maxLength));
  }

  public static InvalidEmailError blank() {
    return new InvalidEmailError(new Reason.Blank());
  }

  public static InvalidEmailError absent() {
    return new InvalidEmailError(new Reason.Absent());
  }

  public sealed interface Reason extends DomainErrorReason {
    record InvalidFormat() implements Reason {}

    record TooLong(int maxLength) implements Reason {}

    record Blank() implements Reason {}

    record Absent() implements Reason {}
  }
}
