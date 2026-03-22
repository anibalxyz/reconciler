package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.features.common.domain.error.DomainErrorReason;
import com.anibalxyz.features.common.domain.error.ReasonedError;

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
    implements UserDomainError {

  public sealed interface Reason extends DomainErrorReason {
    record InvalidFormat() implements Reason {}
  }

  public InvalidEmailError(Reason reason) {
    super(reason);
  }
}
