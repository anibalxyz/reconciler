package com.anibalxyz.features.common.domain.error;

/**
 * Specialized domain error that encapsulates a specific failure reason.
 *
 * <p>Use this base class when an error needs to expose a machine-readable reason (e.g.,
 * distinguishing between 'TOO_SHORT' and 'INVALID_FORMAT' for a password error).
 *
 * @param <R> The type of reason, which must implement {@link DomainErrorReason}.
 */
public abstract class ReasonedError<R extends DomainErrorReason> implements DomainError {
  private final R reason;

  protected ReasonedError(R reason) {
    this.reason = reason;
  }

  public R getReason() {
    return reason;
  }
}
