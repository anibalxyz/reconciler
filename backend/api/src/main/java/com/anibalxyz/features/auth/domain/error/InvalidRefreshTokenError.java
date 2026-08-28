package com.anibalxyz.features.auth.domain.error;

import com.anibalxyz.core.domain.error.DomainErrorReason;
import com.anibalxyz.core.domain.error.ReasonedError;

public final class InvalidRefreshTokenError extends ReasonedError<InvalidRefreshTokenError.Reason>
    implements AuthDomainError.InvalidValueError {

  public InvalidRefreshTokenError(Reason reason) {
    super(reason);
  }

  public static InvalidRefreshTokenError notFound() {
    return new InvalidRefreshTokenError(new Reason.NotFound());
  }

  public static InvalidRefreshTokenError expired() {
    return new InvalidRefreshTokenError(new Reason.Expired());
  }

  public static InvalidRefreshTokenError revoked() {
    return new InvalidRefreshTokenError(new Reason.Revoked());
  }

  public static InvalidRefreshTokenError invalid() {
    return new InvalidRefreshTokenError(new Reason.Invalid());
  }

  public sealed interface Reason extends DomainErrorReason {
    record NotFound() implements Reason {}

    record Expired() implements Reason {}

    record Revoked() implements Reason {}

    record Invalid() implements Reason {}
  }
}
