package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.features.common.domain.error.DomainErrorReason;
import com.anibalxyz.features.common.domain.error.EntityNotFoundError;
import com.anibalxyz.features.common.domain.error.ReasonedError;

public final class UserNotFoundError extends ReasonedError<UserNotFoundError.Reason>
    implements EntityNotFoundError, UserDomainError {
  public sealed interface Reason extends DomainErrorReason {
    record ByEmail(String email) implements Reason {}

    record ById(int id) implements Reason {}
  }

  private UserNotFoundError(Reason reason) {
    super(reason);
  }

  public static UserNotFoundError byId(int id) {
    return new UserNotFoundError(new Reason.ById(id));
  }

  public static UserNotFoundError byEmail(String email) {
    return new UserNotFoundError(new Reason.ByEmail(email));
  }
}
