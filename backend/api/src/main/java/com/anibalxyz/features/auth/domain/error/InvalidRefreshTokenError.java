package com.anibalxyz.features.auth.domain.error;

import com.anibalxyz.features.common.domain.error.DomainErrorReason;
import com.anibalxyz.features.common.domain.error.ReasonedError;

public final class InvalidRefreshTokenError extends ReasonedError<InvalidRefreshTokenError.Reason>
    implements AuthDomainError.InvalidValueError {

  public sealed interface Reason extends DomainErrorReason {
    record NotFound() implements Reason {}

    record Expired() implements Reason {}

    record Revoked() implements Reason {}
  }

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
}
