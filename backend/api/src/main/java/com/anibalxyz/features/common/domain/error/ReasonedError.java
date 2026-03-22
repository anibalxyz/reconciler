package com.anibalxyz.features.common.domain.error;

/**
 * Base class for domain errors caused by an invalid value.
 *
 * <p>Subclasses represent specific value violations (e.g., invalid email, invalid password) and
 * expose a typed {@code getReason()} method that describes why the value is invalid.
 */
public abstract class ReasonedError<R extends DomainErrorReason> extends DomainError {
  public R reason;

  protected ReasonedError(R reason) {
    this.reason = reason;
  }

  public R getReason() {
    return reason;
  }
}
