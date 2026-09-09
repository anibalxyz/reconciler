package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.core.domain.DomainError;

public sealed interface UserDomainError extends DomainError
    permits EmailAlreadyTakenError, UserDomainError.InvalidValueError, UserNotFoundError {
  sealed interface InvalidValueError
      extends com.anibalxyz.core.domain.InvalidValueError, UserDomainError
      permits InvalidEmailError,
          InvalidNameError,
          InvalidPasswordError,
          InvalidPasswordHashError,
          InvalidUserIdError {}
}
